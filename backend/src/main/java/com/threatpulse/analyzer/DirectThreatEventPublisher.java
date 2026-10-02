package com.threatpulse.analyzer;

import com.threatpulse.collector.ThreatEventPublisher;
import com.threatpulse.collector.dto.RawThreatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pipeline without Kafka: the collector hands each raw threat straight to the ingestion
 * service. Alerts are found later by the alert scheduler, so nothing is published here.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class DirectThreatEventPublisher implements ThreatEventPublisher {
    private final ThreatIngestionService threatIngestionService;

    @Override
    public void publish(RawThreatEvent event) {
        threatIngestionService.ingest(event);
    }
}
