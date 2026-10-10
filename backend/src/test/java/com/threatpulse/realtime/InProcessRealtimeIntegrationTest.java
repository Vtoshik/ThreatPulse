package com.threatpulse.realtime;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.dto.ThreatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Live updates in the mode that runs in production: no Kafka, so the pipeline publishes
 * in-process Spring events and {@link RealtimeService} turns them into WebSocket messages.
 * A wrong listener registration would only show up as "nothing arrives" after a deploy.
 */
@TestPropertySource(properties = "app.pipeline.kafka-enabled=false")
public class InProcessRealtimeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ThreatRepository threatRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void analyzedThreatEvent_shouldReachTheThreatsTopic_withoutKafka() {
        String externalId = "inprocess-" + UUID.randomUUID();
        Threat threat = new Threat();
        threat.setExternalId(externalId);
        threat.setTitle("Title");
        threat.setDescription("Description");
        threat.setSourceUrl("https://example.com/" + externalId);
        threat.setSourceName("TEST");
        threat.setSeverity(Severity.HIGH);
        threat.setThreatCategory(ThreatCategory.OTHER);
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAffectedTechnologies(Set.of("spring-boot"));
        threatRepository.save(threat);

        eventPublisher.publishEvent(new AnalyzedThreatEvent(externalId, "title", "desc", "summary",
                "HIGH", "OTHER", List.of(), "action", "https://example.com", "NVD",
                OffsetDateTime.now()));

        ArgumentCaptor<ThreatResponse> captor = ArgumentCaptor.forClass(ThreatResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/threats"), captor.capture());
        assertThat(captor.getValue().externalId()).isEqualTo(externalId);
        assertThat(captor.getValue().affectedTechnologies()).containsExactly("spring-boot");
    }

    @Test
    void alertTriggerEvent_shouldReachThePersonalTopic_withoutKafka() {
        AlertTriggerEvent event = new AlertTriggerEvent(5L, "title", Severity.HIGH, 9L);

        eventPublisher.publishEvent(event);

        verify(messagingTemplate).convertAndSend("/topic/alerts/5", event);
    }
}
