package com.threatpulse.realtime;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.dto.ThreatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Calls the real (transactional) listener with a threat stored in a real database.
 * <p>
 * Mockito unit tests cannot cover this: the technologies of a threat are a lazy collection,
 * and it only fails when the collection is read without an open Hibernate session.
 * The WebSocket template is replaced by a mock so the sent payload can be inspected.
 */
public class RealtimeConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RealtimeConsumer realtimeConsumer;

    @Autowired
    private ThreatRepository threatRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private Threat saveThreatWithTechnologies(String externalId, Set<String> technologies) {
        Threat threat = new Threat();
        threat.setExternalId(externalId);
        threat.setTitle("Title " + externalId);
        threat.setDescription("Description " + externalId);
        threat.setSourceUrl("https://example.com/" + externalId);
        threat.setSourceName("TEST");
        threat.setSeverity(Severity.HIGH);
        threat.setThreatCategory(ThreatCategory.OTHER);
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAffectedTechnologies(technologies);
        return threatRepository.save(threat);
    }

    private AnalyzedThreatEvent eventFor(String externalId) {
        return new AnalyzedThreatEvent(externalId, "title", "desc", "summary",
                "HIGH", "OTHER", List.of(), "action", "https://example.com", "NVD",
                OffsetDateTime.now());
    }

    @Test
    void onAnalyzedThreat_shouldSendResponseWithReadableTechnologies() {
        String externalId = "realtime-" + UUID.randomUUID();
        Threat saved = saveThreatWithTechnologies(externalId, Set.of("spring-boot", "redis"));

        realtimeConsumer.onAnalyzedThreat(eventFor(externalId));

        ArgumentCaptor<ThreatResponse> captor = ArgumentCaptor.forClass(ThreatResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/threats"), captor.capture());
        ThreatResponse sent = captor.getValue();

        assertThat(sent.id()).isEqualTo(saved.getId());
        assertThat(sent.externalId()).isEqualTo(externalId);
        // Read after the listener's transaction has ended, like JSON serialization would.
        // A lazy collection that was never loaded throws LazyInitializationException here.
        assertThat(sent.affectedTechnologies()).containsExactlyInAnyOrder("spring-boot", "redis");
    }
}
