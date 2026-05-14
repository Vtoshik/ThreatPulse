package com.threatpulse.realtime;

import com.threatpulse.common.domain.Threat;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimeConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeConsumer (SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "analyzed-threats", groupId = "realtime-group")
    public void onAnalyzedThreat(Threat threat) {
        messagingTemplate.convertAndSend("/topic/threats", threat);
    }
}
