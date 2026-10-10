package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Receives analyzed threats and alert triggers from Kafka and hands them to the
 * {@link RealtimeService}, which talks to the browsers.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeConsumer {
    private final RealtimeService realtimeService;

    @KafkaListener(topics = KafkaConfig.ANALYZED_THREATS_TOPIC, groupId = "realtime-group")
    public void onAnalyzedThreat(AnalyzedThreatEvent event) {
        realtimeService.publishThreat(event.externalId());
    }

    @KafkaListener(topics = KafkaConfig.ALERT_TRIGGERS_TOPIC, groupId = "realtime-alerts-group")
    public void onAlertTrigger(AlertTriggerEvent event) {
        realtimeService.publishAlert(event);
    }
}
