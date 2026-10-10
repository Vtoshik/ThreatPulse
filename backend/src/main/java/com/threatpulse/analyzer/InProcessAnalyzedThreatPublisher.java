package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Used when Kafka is switched off. The analyzed threat is published as an in-process event,
 * so live updates reach the browsers the same way as with Kafka. Alerts are not triggered
 * from here: the alert scheduler checks the threats that were analyzed recently, and a
 * threat gets its analysis time when it is saved or when a failed analysis succeeds later.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class InProcessAnalyzedThreatPublisher implements AnalyzedThreatPublisher {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(AnalyzedThreatEvent event) {
        eventPublisher.publishEvent(event);
    }
}
