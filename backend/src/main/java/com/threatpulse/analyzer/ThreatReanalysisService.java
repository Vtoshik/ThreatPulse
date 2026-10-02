package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repairs threats that were saved without a finished analysis or without an embedding:
 * <ul>
 *   <li>repeats the AI analysis of threats that are PENDING_ANALYSIS</li>
 *   <li>creates the embedding of analyzed threats that have none</li>
 * </ul>
 * Calls to Groq are paced by the throttle of the Groq client, so this job and the normal
 * pipeline together stay below the rate limit.
 * <p>
 * Database work is kept out of long transactions on purpose: every threat is saved on its own,
 * so no connection is held while waiting for a slow external API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatReanalysisService {
    /** After this many failures in a row Groq is probably limited or down, so the run stops. */
    static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final ThreatRepository threatRepository;
    private final ThreatAnalyzer threatAnalyzer;
    private final ThreatMapper threatMapper;
    private final EmbeddingService embeddingService;
    private final AnalyzedThreatPublisher analyzedThreatPublisher;

    /**
     * Tries to analyze up to batchSize pending threats.
     *
     * @return how many threats were analyzed
     */
    @CacheEvict(value = "threats", allEntries = true)
    public int analyzePending(int batchSize) {
        List<Threat> pending = threatRepository.findPendingForAnalysis(PageRequest.of(0, batchSize));
        int analyzedCount = 0;
        int consecutiveFailures = 0;

        for (Threat threat : pending) {
            // Every attempt counts, also a failed one: it sends the threat to the back of the queue
            threat.setAnalysisAttemptedAt(OffsetDateTime.now());

            Optional<AnalyzedThreatEvent> result = threatAnalyzer.analyze(threat);

            if (result.isEmpty()) {
                threatRepository.save(threat);
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.warn("Re-analysis stopped after {} failures in a row, will try again later",
                            consecutiveFailures);
                    break;
                }
                continue;
            }

            consecutiveFailures = 0;
            AnalyzedThreatEvent analyzed = result.get();
            threatMapper.applyAnalysis(threat, analyzed);
            // A missing embedding (null) is allowed here, the backfill below fills it in later
            threat.setEmbedding(embeddingService.embedDocument(ThreatMapper.embeddingText(threat)));
            threatRepository.save(threat);

            // Now alerts and live updates may react, exactly as for a threat analyzed right away
            analyzedThreatPublisher.publish(analyzed);
            analyzedCount++;
        }

        return analyzedCount;
    }

    /**
     * Creates the embedding for up to batchSize analyzed threats that have none.
     * Stops at the first failure, because that usually means the embedding service is down
     * or its free credit is used up.
     *
     * @return how many embeddings were created
     */
    public int backfillEmbeddings(int batchSize) {
        List<Threat> threats = threatRepository.findAnalyzedWithoutEmbedding(PageRequest.of(0, batchSize));
        int filled = 0;

        for (Threat threat : threats) {
            float[] embedding = embeddingService.embedDocument(ThreatMapper.embeddingText(threat));
            if (embedding == null) {
                log.warn("Embedding not available, stopping the backfill for this run");
                break;
            }
            threat.setEmbedding(embedding);
            threatRepository.save(threat);
            filled++;
        }

        return filled;
    }
}
