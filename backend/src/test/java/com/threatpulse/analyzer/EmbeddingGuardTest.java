package com.threatpulse.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.threatpulse.analyzer.EmbeddingGuard.Kind.DOCUMENT;
import static com.threatpulse.analyzer.EmbeddingGuard.Kind.QUERY;
import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingGuardTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-02T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private EmbeddingGuard guard;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        guard = new EmbeddingGuard(clock, Duration.ofHours(12), 3, 2);
    }

    @Test
    void tryAcquire_shouldAllowCalls_whenNothingWentWrong() {
        assertThat(guard.tryAcquire(DOCUMENT)).isTrue();
        assertThat(guard.tryAcquire(QUERY)).isTrue();
    }

    @Test
    void outOfCredits_shouldPauseForTheConfiguredTime_thenAllowAgain() {
        guard.recordFailure(402, null);

        assertThat(guard.tryAcquire(DOCUMENT)).isFalse();
        assertThat(guard.tryAcquire(QUERY)).isFalse();

        clock.advance(Duration.ofHours(11));
        assertThat(guard.tryAcquire(DOCUMENT)).isFalse();

        clock.advance(Duration.ofHours(2));
        assertThat(guard.tryAcquire(DOCUMENT)).isTrue();
    }

    @Test
    void invalidKey_shouldPauseForADay() {
        guard.recordFailure(401, null);

        clock.advance(Duration.ofHours(23));
        assertThat(guard.tryAcquire(QUERY)).isFalse();

        clock.advance(Duration.ofHours(2));
        assertThat(guard.tryAcquire(QUERY)).isTrue();
    }

    @Test
    void rateLimit_shouldPauseForTheTimeTheProviderAsksFor() {
        guard.recordFailure(429, Duration.ofSeconds(90));

        clock.advance(Duration.ofSeconds(60));
        assertThat(guard.tryAcquire(QUERY)).isFalse();

        clock.advance(Duration.ofSeconds(40));
        assertThat(guard.tryAcquire(QUERY)).isTrue();
    }

    @Test
    void rateLimit_shouldUseADefaultPause_whenNoTimeIsGiven() {
        guard.recordFailure(429, null);

        clock.advance(Duration.ofMinutes(4));
        assertThat(guard.isAvailable(QUERY)).isFalse();

        clock.advance(Duration.ofMinutes(2));
        assertThat(guard.isAvailable(QUERY)).isTrue();
    }

    @Test
    void rateLimit_shouldCapAnUnreasonablyLongRequest() {
        guard.recordFailure(429, Duration.ofDays(30));

        clock.advance(EmbeddingGuard.MAX_RATE_LIMIT_PAUSE.plusMinutes(1));

        assertThat(guard.isAvailable(QUERY)).isTrue();
    }

    @Test
    void temporaryFailures_shouldPauseOnlyAfterSeveralInARow() {
        guard.recordFailure(503, null);
        guard.recordFailure(0, null);
        assertThat(guard.isAvailable(DOCUMENT)).isTrue();

        guard.recordFailure(500, null);

        assertThat(guard.isAvailable(DOCUMENT)).isFalse();
        clock.advance(EmbeddingGuard.TEMPORARY_PAUSE.plusSeconds(1));
        assertThat(guard.isAvailable(DOCUMENT)).isTrue();
    }

    @Test
    void aSuccess_shouldResetTheCountOfTemporaryFailures() {
        guard.recordFailure(503, null);
        guard.recordFailure(503, null);
        guard.recordSuccess();
        guard.recordFailure(503, null);
        guard.recordFailure(503, null);

        assertThat(guard.isAvailable(DOCUMENT)).isTrue();
    }

    @Test
    void aShorterPause_shouldNotCutALongerOneShort() {
        guard.recordFailure(402, null);
        guard.recordFailure(429, Duration.ofSeconds(30));

        clock.advance(Duration.ofHours(1));

        assertThat(guard.isAvailable(DOCUMENT)).isFalse();
    }

    @Test
    void dailyBudget_shouldBeSeparateForDocumentsAndQueries() {
        for (int i = 0; i < 3; i++) {
            assertThat(guard.tryAcquire(DOCUMENT)).isTrue();
        }
        assertThat(guard.tryAcquire(DOCUMENT)).isFalse();

        // The documents used up their budget, search still has its own
        assertThat(guard.tryAcquire(QUERY)).isTrue();
        assertThat(guard.tryAcquire(QUERY)).isTrue();
        assertThat(guard.tryAcquire(QUERY)).isFalse();
    }

    @Test
    void dailyBudget_shouldStartAgainAtMidnightUtc() {
        for (int i = 0; i < 3; i++) {
            guard.tryAcquire(DOCUMENT);
        }
        assertThat(guard.tryAcquire(DOCUMENT)).isFalse();

        clock.advance(Duration.ofHours(14)); // 10:00 -> 00:00 the next day

        assertThat(guard.tryAcquire(DOCUMENT)).isTrue();
    }

    @Test
    void isAvailable_shouldNotUseUpTheBudget() {
        for (int i = 0; i < 10; i++) {
            assertThat(guard.isAvailable(QUERY)).isTrue();
        }

        assertThat(guard.tryAcquire(QUERY)).isTrue();
        assertThat(guard.tryAcquire(QUERY)).isTrue();
    }

    @Test
    void zeroLimit_shouldMeanNoLimit() {
        EmbeddingGuard noLimit = new EmbeddingGuard(clock, Duration.ofHours(12), 0, 0);

        for (int i = 0; i < 1000; i++) {
            assertThat(noLimit.tryAcquire(DOCUMENT)).isTrue();
        }
    }
}
