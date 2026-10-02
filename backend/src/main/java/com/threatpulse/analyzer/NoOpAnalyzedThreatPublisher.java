package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Used when Kafka is switched off. Nothing needs to be sent: the alert scheduler checks the
 * threats that were analyzed recently, and a threat gets its analysis time when it is saved
 * or when a failed analysis is repeated successfully.
 */
@Component
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class NoOpAnalyzedThreatPublisher implements AnalyzedThreatPublisher {

    @Override
    public void publish(AnalyzedThreatEvent event) {
        // intentionally empty
    }
}
