package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyzerConsumerTest {

    @Mock private ThreatAnalyzer threatAnalyzer;
    @Mock private ThreatRepository threatRepository;
    @Mock private KafkaTemplate<String, AnalyzedThreatEvent> kafkaTemplate;
    @Mock private ThreatMapper threatMapper;
    @Mock private EmbeddingService embeddingService;

    @InjectMocks
    private AnalyzerConsumer analyzerConsumer;

    private RawThreatEvent buildRawEvent(String externalId) {
        return new RawThreatEvent(externalId, "title", "desc",
                "https://example.com", "NVD", OffsetDateTime.now(), "CVE");
    }

    private AnalyzedThreatEvent buildAnalyzedEvent(String externalId) {
        return new AnalyzedThreatEvent(externalId, "title", "desc",
                "summary", "HIGH", "OTHER", List.of("spring-boot"),
                "action", "https://example.com", "NVD", OffsetDateTime.now());
    }

    private Threat buildThreat() {
        return new Threat();
    }

    @Test
    void consume_shouldSkipSaving_whenThreatAlreadyExists() {
        when(threatRepository.existsByExternalId("ext-1")).thenReturn(true);

        analyzerConsumer.consume(buildRawEvent("ext-1"));

        // existsByExternalId returned true — save and kafka publish must not happen
        verify(threatRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void consume_shouldSaveAndPublish_whenThreatIsNew() {
        when(threatRepository.existsByExternalId("ext-2")).thenReturn(false);
        when(threatAnalyzer.analyze(any())).thenReturn(buildAnalyzedEvent("ext-2"));
        when(threatMapper.toThreat(any())).thenReturn(buildThreat());

        analyzerConsumer.consume(buildRawEvent("ext-2"));

        verify(threatRepository).save(any(Threat.class));
        verify(kafkaTemplate).send(any(), eq("ext-2"), any(AnalyzedThreatEvent.class));
    }
}