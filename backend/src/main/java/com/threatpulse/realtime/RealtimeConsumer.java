package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.ThreatResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeConsumer {
    private final SimpMessagingTemplate messagingTemplate;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;

    @Transactional(readOnly = true)
    @KafkaListener(topics = KafkaConfig.ANALYZED_THREATS_TOPIC, groupId = "realtime-group")
    public void onAnalyzedThreat(AnalyzedThreatEvent event) {
        threatRepository.findByExternalId(event.externalId())
                .map(threatResponseMapper::toThreatResponse)
                .ifPresentOrElse(
                        response ->
                                messagingTemplate.convertAndSend(
                                        "/topic/threats",
                                        response),
                        () -> log.warn("The threat with externalId: {} " +
                                "wasn't found in the database", event.externalId())
                );
    }

    @KafkaListener(topics = KafkaConfig.ALERT_TRIGGERS_TOPIC, groupId = "realtime-alerts-group")
    public void onAlertTrigger(AlertTriggerEvent event) {
        messagingTemplate.convertAndSend("/topic/alerts/" + event.userId(), event);
    }
}
