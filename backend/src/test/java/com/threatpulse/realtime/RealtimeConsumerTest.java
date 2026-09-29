package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.ThreatResponseMapper;
import com.threatpulse.feed.dto.ThreatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RealtimeConsumerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ThreatRepository threatRepository;
    @Mock private ThreatResponseMapper threatResponseMapper;

    @InjectMocks
    private RealtimeConsumer realtimeConsumer;

    private AnalyzedThreatEvent buildEvent(String externalId) {
        return new AnalyzedThreatEvent(externalId, "title", "desc", "summary",
                "HIGH", "OTHER", List.of("spring-boot"),
                "action", "https://example.com", "NVD", OffsetDateTime.now());
    }

    private ThreatResponse buildResponse(String externalId) {
        return new ThreatResponse(1L, externalId, "title", "desc", "summary",
                Severity.HIGH, ThreatCategory.OTHER, "NVD", "https://example.com",
                OffsetDateTime.now(), Set.of("spring-boot"));
    }

    @Test
    void onAnalyzedThreat_shouldSendMappedResponseToThreatsTopic_whenThreatExists() {
        Threat threat = new Threat();
        ThreatResponse response = buildResponse("ext-1");
        when(threatRepository.findByExternalId("ext-1")).thenReturn(Optional.of(threat));
        when(threatResponseMapper.toThreatResponse(threat)).thenReturn(response);

        realtimeConsumer.onAnalyzedThreat(buildEvent("ext-1"));

        // The payload must be the DTO, not the entity and not an Optional
        verify(messagingTemplate).convertAndSend("/topic/threats", response);
    }

    @Test
    void onAnalyzedThreat_shouldNotSendAnything_whenThreatIsNotFound() {
        when(threatRepository.findByExternalId("missing")).thenReturn(Optional.empty());

        realtimeConsumer.onAnalyzedThreat(buildEvent("missing"));

        verifyNoInteractions(messagingTemplate);
        verifyNoInteractions(threatResponseMapper);
    }

    @Test
    void onAlertTrigger_shouldSendEventToTheUsersPersonalTopic() {
        AlertTriggerEvent event = new AlertTriggerEvent(42L, "title", Severity.HIGH, 7L);

        realtimeConsumer.onAlertTrigger(event);

        verify(messagingTemplate).convertAndSend("/topic/alerts/42", event);
    }
}
