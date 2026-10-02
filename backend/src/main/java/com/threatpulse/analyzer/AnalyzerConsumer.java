package com.threatpulse.analyzer;

import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class AnalyzerConsumer {
    private final ThreatIngestionService threatIngestionService;
    private final AnalyzedThreatPublisher analyzedThreatPublisher;

    @KafkaListener(topics = KafkaConfig.RAW_THREATS_TOPIC, groupId = "analyzer-group")
    public void consume(RawThreatEvent event) {
        // Only a threat saved with a finished analysis is announced. A pending one is not.
        threatIngestionService.ingest(event).ifPresent(analyzed -> {
            analyzedThreatPublisher.publish(analyzed);
            log.info("Threat published: {}", analyzed.externalId());
        });
    }
}
