package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyzerConsumerTest {

    @Mock private ThreatIngestionService threatIngestionService;
    @Mock private AnalyzedThreatPublisher analyzedThreatPublisher;

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

    @Test
    void consume_shouldPublishTheAnalyzedThreat_whenIngestionReturnsIt() {
        RawThreatEvent raw = buildRawEvent("ext-1");
        AnalyzedThreatEvent analyzed = buildAnalyzedEvent("ext-1");
        when(threatIngestionService.ingest(raw)).thenReturn(Optional.of(analyzed));

        analyzerConsumer.consume(raw);

        verify(analyzedThreatPublisher).publish(analyzed);
    }

    @Test
    void consume_shouldNotPublishAnything_whenIngestionReturnsNothing() {
        // The threat already existed, or its analysis failed and it was saved as pending
        RawThreatEvent raw = buildRawEvent("ext-2");
        when(threatIngestionService.ingest(raw)).thenReturn(Optional.empty());

        analyzerConsumer.consume(raw);

        verify(analyzedThreatPublisher, never()).publish(any());
    }
}
