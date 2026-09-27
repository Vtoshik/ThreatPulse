package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class AnalyzerConsumer {
    private final ThreatAnalyzer threatAnalyzer;
    private final ThreatRepository threatRepository;
    private final KafkaTemplate<String, AnalyzedThreatEvent> kafkaTemplate;
    private final ThreatMapper threatMapper;
    private final EmbeddingService embeddingService;

    @CacheEvict(value = "threats", allEntries = true)
    @KafkaListener(topics = KafkaConfig.RAW_THREATS_TOPIC, groupId = "analyzer-group")
    public void consume(RawThreatEvent event) {
        if (threatRepository.existsByExternalId(event.externalId())) {
            log.info("Threat already exists, skipping: {}", event.externalId());
            return;
        }

        log.info("Analyzing threat: {}", event.externalId());

        AnalyzedThreatEvent analyzed = threatAnalyzer.analyze(event);

        Threat threat = threatMapper.toThreat(analyzed);
        threat.setEmbedding(embeddingService.embedDocument(ThreatMapper.embeddingText(analyzed)));

        threatRepository.save(threat);
        kafkaTemplate.send(KafkaConfig.ANALYZED_THREATS_TOPIC, analyzed.externalId(), analyzed);
        log.info("Threat saved and published: {}", analyzed.externalId());
    }
}
