package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.ThreatEventPublisher;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class DirectThreatEventPublisher implements ThreatEventPublisher {
    private final ThreatAnalyzer threatAnalyzer;
    private final ThreatRepository threatRepository;
    private final ThreatMapper threatMapper;
    private final EmbeddingService embeddingService;

    @Override
    @CacheEvict(value = "threats", allEntries = true)
    public void publish(RawThreatEvent event) {
        if (threatRepository.existsByExternalId(event.externalId())) {
            log.info("Threat already exists, skipping: {}", event.externalId());
            return;
        }

        log.info("Analyzing threat (direct pipeline): {}", event.externalId());

        AnalyzedThreatEvent analyzed = threatAnalyzer.analyze(event);

        Threat threat = threatMapper.toThreat(analyzed);
        threat.setEmbedding(embeddingService.embedDocument(ThreatMapper.embeddingText(analyzed)));

        threatRepository.save(threat);
        log.info("Threat saved: {}", analyzed.externalId());
    }
}