package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends analyzed threats to Kafka. The alert, realtime and feed consumers listen to this topic.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAnalyzedThreatPublisher implements AnalyzedThreatPublisher {
    private final KafkaTemplate<String, AnalyzedThreatEvent> kafkaTemplate;

    @Override
    public void publish(AnalyzedThreatEvent event) {
        kafkaTemplate.send(KafkaConfig.ANALYZED_THREATS_TOPIC, event.externalId(), event);
    }
}
