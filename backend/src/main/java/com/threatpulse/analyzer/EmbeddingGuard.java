package com.threatpulse.analyzer;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Protects the free credit of the embedding provider.
 * <p>
 * Every call that succeeds costs a little credit, and a call that fails is retried by the
 * scheduled backfill forever. Two rules stop that:
 * <ul>
 *   <li><b>Pause after an error that will not fix itself soon.</b> "No credit left" (402) pauses
 *       for hours, an invalid key (401/403) for a day, a rate limit (429) for the time the
 *       provider asks. Three temporary failures in a row (timeout, 5xx) pause for a few minutes.
 *       While paused no request is sent at all.</li>
 *   <li><b>A daily budget</b> for document embeddings (new threats and the backfill) and a
 *       separate one for search queries. The backfill can therefore never use up the credit
 *       that search needs, and a bug cannot turn into a large bill.</li>
 * </ul>
 * The state lives in memory: a restart gives a fresh budget, and a fresh start costs at most
 * one failed call per cause, because the first answer pauses the guard again.
 */
@Slf4j
final class EmbeddingGuard {
    enum Kind { DOCUMENT, QUERY }

    static final Duration INVALID_KEY_PAUSE = Duration.ofHours(24);
    static final Duration DEFAULT_RATE_LIMIT_PAUSE = Duration.ofMinutes(5);
    static final Duration MAX_RATE_LIMIT_PAUSE = Duration.ofHours(6);
    static final Duration TEMPORARY_PAUSE = Duration.ofMinutes(5);
    static final int TEMPORARY_FAILURES_BEFORE_PAUSE = 3;

    private final Clock clock;
    private final Duration outOfCreditsPause;
    private final int maxDocumentsPerDay;
    private final int maxQueriesPerDay;

    private Instant pausedUntil = Instant.MIN;
    private int temporaryFailures;
    private LocalDate day;
    private int documentsToday;
    private int queriesToday;

    /** A limit of zero or less means no limit. */
    EmbeddingGuard(Clock clock, Duration outOfCreditsPause, int maxDocumentsPerDay, int maxQueriesPerDay) {
        this.clock = clock;
        this.outOfCreditsPause = outOfCreditsPause;
        this.maxDocumentsPerDay = maxDocumentsPerDay;
        this.maxQueriesPerDay = maxQueriesPerDay;
    }

    /** No pause rules beyond the defaults and no daily limits. Used where the budget does not matter. */
    static EmbeddingGuard unlimited() {
        return new EmbeddingGuard(Clock.systemUTC(), Duration.ofHours(12), 0, 0);
    }

    /**
     * Asks for permission to send one request and counts it against the daily budget.
     *
     * @return false while paused or when today's budget of this kind is used up
     */
    synchronized boolean tryAcquire(Kind kind) {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            return false;
        }
        startNewDayIfNeeded(now);

        if (kind == Kind.DOCUMENT) {
            if (maxDocumentsPerDay > 0 && documentsToday >= maxDocumentsPerDay) {
                return false;
            }
            documentsToday++;
            if (maxDocumentsPerDay > 0 && documentsToday == maxDocumentsPerDay) {
                log.warn("Embedding budget for documents is used up ({} today), no more until midnight UTC",
                        maxDocumentsPerDay);
            }
        } else {
            if (maxQueriesPerDay > 0 && queriesToday >= maxQueriesPerDay) {
                return false;
            }
            queriesToday++;
            if (maxQueriesPerDay > 0 && queriesToday == maxQueriesPerDay) {
                log.warn("Embedding budget for search queries is used up ({} today), no more until midnight UTC",
                        maxQueriesPerDay);
            }
        }
        return true;
    }

    synchronized void recordSuccess() {
        temporaryFailures = 0;
    }

    /**
     * @param httpStatus the status the provider answered, or 0 for a network error or timeout
     * @param retryAfter the pause the provider asked for, or null
     */
    synchronized void recordFailure(int httpStatus, Duration retryAfter) {
        Duration pause = null;
        String reason = null;

        if (httpStatus == 402) {
            pause = outOfCreditsPause;
            reason = "the credit is used up (HTTP 402)";
        } else if (httpStatus == 401 || httpStatus == 403) {
            pause = INVALID_KEY_PAUSE;
            reason = "the API key is not accepted (HTTP " + httpStatus + ")";
        } else if (httpStatus == 429) {
            pause = retryAfter == null ? DEFAULT_RATE_LIMIT_PAUSE
                    : retryAfter.compareTo(MAX_RATE_LIMIT_PAUSE) > 0 ? MAX_RATE_LIMIT_PAUSE : retryAfter;
            reason = "rate limited (HTTP 429)";
        } else {
            temporaryFailures++;
            if (temporaryFailures >= TEMPORARY_FAILURES_BEFORE_PAUSE) {
                temporaryFailures = 0;
                pause = TEMPORARY_PAUSE;
                reason = "too many failures in a row";
            }
        }

        if (pause != null) {
            Instant until = clock.instant().plus(pause);
            // A longer pause already running wins over a shorter new one
            if (until.isAfter(pausedUntil)) {
                pausedUntil = until;
                log.warn("Embeddings paused until {}: {}", until, reason);
            }
        }
    }

    /** True when a request would be allowed right now. Does not count against the budget. */
    synchronized boolean isAvailable(Kind kind) {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            return false;
        }
        startNewDayIfNeeded(now);
        return kind == Kind.DOCUMENT
                ? maxDocumentsPerDay <= 0 || documentsToday < maxDocumentsPerDay
                : maxQueriesPerDay <= 0 || queriesToday < maxQueriesPerDay;
    }

    private void startNewDayIfNeeded(Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (!today.equals(day)) {
            day = today;
            documentsToday = 0;
            queriesToday = 0;
        }
    }
}
