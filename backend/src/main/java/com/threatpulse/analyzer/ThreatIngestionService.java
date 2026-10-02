package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The one place that turns a raw threat into a stored one, shared by the Kafka consumer and the
 * direct (no Kafka) pipeline.
 * <p>
 * If the AI analysis fails, the threat is still saved, but as PENDING_ANALYSIS and without any
 * made-up values, and nothing is published. A later job repeats the analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatIngestionService {
    private final ThreatRepository threatRepository;
    private final ThreatAnalyzer threatAnalyzer;
    private final ThreatMapper threatMapper;
    private final EmbeddingService embeddingService;

    /**
     * @return the analyzed threat when it was saved with a finished analysis and should be
     *         announced to alerts and live updates, empty when it already existed or its
     *         analysis failed
     */
    @CacheEvict(value = "threats", allEntries = true)
    public Optional<AnalyzedThreatEvent> ingest(RawThreatEvent raw) {
        if (threatRepository.existsByExternalId(raw.externalId())) {
            log.info("Threat already exists, skipping: {}", raw.externalId());
            return Optional.empty();
        }

        log.info("Analyzing threat: {}", raw.externalId());
        Optional<AnalyzedThreatEvent> analyzed = threatAnalyzer.analyze(raw);

        if (analyzed.isEmpty()) {
            threatRepository.save(threatMapper.toPendingThreat(raw));
            log.warn("Analysis failed, threat saved as pending: {}", raw.externalId());
            return Optional.empty();
        }

        Threat threat = threatMapper.toThreat(analyzed.get());
        // A missing embedding (null) is allowed: the re-analysis job fills it in later
        threat.setEmbedding(embeddingService.embedDocument(
                ThreatMapper.embeddingText(analyzed.get())));
        threatRepository.save(threat);
        log.info("Threat saved: {}", raw.externalId());

        return analyzed;
    }
}
