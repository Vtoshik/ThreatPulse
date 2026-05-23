package com.threatpulse.collector;

import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaThreatEventPublisher implements ThreatEventPublisher {
    private final KafkaTemplate<String, RawThreatEvent> kafkaTemplate;

    @Override
    public void publish(RawThreatEvent event) {
        kafkaTemplate.send(KafkaConfig.RAW_THREATS_TOPIC, event.externalId(), event);
    }
}