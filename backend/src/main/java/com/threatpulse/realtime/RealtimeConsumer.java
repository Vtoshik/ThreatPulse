package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.common.domain.Threat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeConsumer (SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "analyzed-threats", groupId = "realtime-group")
    public void onAnalyzedThreat(Threat threat) {
        messagingTemplate.convertAndSend("/topic/threats", threat);
    }

    @KafkaListener(topics = KafkaConfig.ALERT_TRIGGERS_TOPIC, groupId = "realtime-alerts-group")
    public void onAlertTrigger(AlertTriggerEvent event) {
        messagingTemplate.convertAndSend("/topic/alerts/" + event.userId(), event);
    }
}
