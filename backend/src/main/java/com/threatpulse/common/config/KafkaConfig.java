package com.threatpulse.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String RAW_THREATS_TOPIC = "raw-threats";
    public static final String ANALYZED_THREATS_TOPIC = "analyzed-threats";
    public static final String ALERT_TRIGGERS_TOPIC = "alert-triggers";

    @Bean
    public NewTopic rawThreatsTopic() {
        return TopicBuilder.name(RAW_THREATS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic analyzedThreatsTopic() {
        return TopicBuilder.name(ANALYZED_THREATS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertTriggersTopic() {
        return TopicBuilder.name(ALERT_TRIGGERS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
