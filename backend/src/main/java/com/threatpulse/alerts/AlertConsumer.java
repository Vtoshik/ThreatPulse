package com.threatpulse.alerts;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertConsumer {
    private final AlertService alertService;

    @KafkaListener(topics = KafkaConfig.ANALYZED_THREATS_TOPIC, groupId = "alerts-group")
    public void consume(AnalyzedThreatEvent event) {
        alertService.checkAndNotify(event);
    }
}
