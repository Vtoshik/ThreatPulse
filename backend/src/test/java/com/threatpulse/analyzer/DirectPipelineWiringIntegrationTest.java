package com.threatpulse.analyzer;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.alerts.AlertScheduler;
import com.threatpulse.collector.ThreatEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the whole application the way it runs in production without Kafka
 * (app.pipeline.kafka-enabled=false). Every other integration test uses the Kafka mode,
 * so a wiring mistake in this mode would otherwise only show up after a deploy.
 */
@TestPropertySource(properties = "app.pipeline.kafka-enabled=false")
public class DirectPipelineWiringIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void directPipeline_shouldBeWiredWithoutKafkaConsumers() {
        assertThat(context.getBean(ThreatEventPublisher.class))
                .isInstanceOf(DirectThreatEventPublisher.class);
        assertThat(context.getBean(AnalyzedThreatPublisher.class))
                .isInstanceOf(NoOpAnalyzedThreatPublisher.class);
        assertThat(context.getBeansOfType(AlertScheduler.class)).hasSize(1);
        assertThat(context.getBeansOfType(AnalyzerConsumer.class)).isEmpty();
    }
}
