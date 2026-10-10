package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RealtimeConsumerTest {
    @Mock private RealtimeService realtimeService;

    @InjectMocks
    private RealtimeConsumer realtimeConsumer;

    @Test
    void onAnalyzedThreat_shouldHandTheExternalIdToTheService() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent("ext-1", "title", "desc", "summary",
                "HIGH", "OTHER", List.of("spring-boot"), "action", "https://example.com",
                "NVD", OffsetDateTime.now());

        realtimeConsumer.onAnalyzedThreat(event);

        verify(realtimeService).publishThreat("ext-1");
    }

    @Test
    void onAlertTrigger_shouldHandTheEventToTheService() {
        AlertTriggerEvent event = new AlertTriggerEvent(42L, "title", Severity.HIGH, 7L);

        realtimeConsumer.onAlertTrigger(event);

        verify(realtimeService).publishAlert(event);
    }
}
