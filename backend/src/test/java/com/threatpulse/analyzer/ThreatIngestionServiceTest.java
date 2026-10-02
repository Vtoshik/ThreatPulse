package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Uses the real ThreatMapper so the saved Threat can be inspected, and mocks everything
 * that talks to the database or to an external API.
 */
@ExtendWith(MockitoExtension.class)
public class ThreatIngestionServiceTest {

    @Mock private ThreatRepository threatRepository;
    @Mock private ThreatAnalyzer threatAnalyzer;
    @Mock private EmbeddingService embeddingService;

    private final ThreatMapper threatMapper = new ThreatMapper();

    private ThreatIngestionService service() {
        return new ThreatIngestionService(threatRepository, threatAnalyzer, threatMapper,
                embeddingService);
    }

    private RawThreatEvent raw(String externalId) {
        return new RawThreatEvent(externalId, "Title", "Description",
                "https://example.com", "NVD", OffsetDateTime.now(), "CVE");
    }

    private AnalyzedThreatEvent analyzed(String externalId) {
        return new AnalyzedThreatEvent(externalId, "Title", "Description", "Summary",
                "CRITICAL", "RCE", List.of("spring-boot"), "Patch",
                "https://example.com", "NVD", OffsetDateTime.now());
    }

    private Threat savedThreat() {
        ArgumentCaptor<Threat> captor = ArgumentCaptor.forClass(Threat.class);
        verify(threatRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void ingest_shouldSkipEverything_whenThreatAlreadyExists() {
        when(threatRepository.existsByExternalId("ext-1")).thenReturn(true);

        Optional<AnalyzedThreatEvent> result = service().ingest(raw("ext-1"));

        assertThat(result).isEmpty();
        verifyNoInteractions(threatAnalyzer, embeddingService);
        verify(threatRepository, never()).save(any());
    }

    @Test
    void ingest_shouldSaveAnalyzedThreatWithEmbedding_andReturnTheEvent() {
        float[] embedding = new float[]{0.1f, 0.2f};
        AnalyzedThreatEvent event = analyzed("ext-2");
        when(threatRepository.existsByExternalId("ext-2")).thenReturn(false);
        when(threatAnalyzer.analyze(any(RawThreatEvent.class))).thenReturn(Optional.of(event));
        when(embeddingService.embedDocument("Title\nSummary")).thenReturn(embedding);

        Optional<AnalyzedThreatEvent> result = service().ingest(raw("ext-2"));

        assertThat(result).contains(event);
        Threat saved = savedThreat();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(saved.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(saved.getAiSummary()).isEqualTo("Summary");
        assertThat(saved.getEmbedding()).isEqualTo(embedding);
        assertThat(saved.getAnalyzedAt()).isNotNull();
    }

    @Test
    void ingest_shouldStillSaveAnalyzedThreat_whenEmbeddingFails() {
        // The embedding is filled in later by the re-analysis job
        when(threatRepository.existsByExternalId("ext-3")).thenReturn(false);
        when(threatAnalyzer.analyze(any(RawThreatEvent.class))).thenReturn(Optional.of(analyzed("ext-3")));
        when(embeddingService.embedDocument(any())).thenReturn(null);

        Optional<AnalyzedThreatEvent> result = service().ingest(raw("ext-3"));

        assertThat(result).isPresent();
        Threat saved = savedThreat();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(saved.getEmbedding()).isNull();
    }

    @Test
    void ingest_shouldSaveAsPendingWithoutMadeUpValues_whenAnalysisFails() {
        when(threatRepository.existsByExternalId("ext-4")).thenReturn(false);
        when(threatAnalyzer.analyze(any(RawThreatEvent.class))).thenReturn(Optional.empty());

        Optional<AnalyzedThreatEvent> result = service().ingest(raw("ext-4"));

        // Nothing is returned, so nothing is published to alerts or live updates
        assertThat(result).isEmpty();
        Threat saved = savedThreat();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(saved.getExternalId()).isEqualTo("ext-4");
        assertThat(saved.getTitle()).isEqualTo("Title");
        assertThat(saved.getSeverity()).isNull();
        assertThat(saved.getThreatCategory()).isNull();
        assertThat(saved.getAiSummary()).isNull();
        assertThat(saved.getAnalyzedAt()).isNull();
        assertThat(saved.getAnalysisAttemptedAt()).isNotNull();
        verifyNoInteractions(embeddingService);
    }
}
