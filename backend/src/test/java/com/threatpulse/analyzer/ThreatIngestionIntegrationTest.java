package com.threatpulse.analyzer;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Runs the ingestion service against a real database. Groq and Hugging Face are replaced by
 * mocks, so the result of a failed analysis is exactly what really gets stored.
 */
public class ThreatIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired private ThreatIngestionService threatIngestionService;
    @Autowired private ThreatRepository threatRepository;

    @MockitoBean private GroqAiClient groqAiClient;
    @MockitoBean private EmbeddingService embeddingService;

    private RawThreatEvent raw(String externalId) {
        return new RawThreatEvent(externalId, "Title " + externalId, "Description",
                "https://example.com/" + externalId, "NVD", OffsetDateTime.now(), "CVE");
    }

    private ThreatAnalysis analysis() {
        ThreatAnalysis analysis = new ThreatAnalysis();
        analysis.setSummary("A summary");
        analysis.setSeverity("CRITICAL");
        analysis.setCategory("SUPPLY_CHAIN");
        analysis.setAffectedTechnologies(List.of("npm", "node.js"));
        analysis.setRecommendedAction("Remove the package");
        return analysis;
    }

    @Test
    void ingest_shouldStoreAnalyzedThreatWithTechnologies_whenAnalysisWorks() {
        String id = "ingest-ok-" + UUID.randomUUID();
        when(groqAiClient.analyze(any(), any())).thenReturn(analysis());
        when(embeddingService.embedDocument(any())).thenReturn(new float[384]);

        Optional<AnalyzedThreatEvent> result = threatIngestionService.ingest(raw(id));

        assertThat(result).isPresent();
        Threat stored = threatRepository.findByExternalId(id).orElseThrow();
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(stored.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(stored.getEmbedding()).hasSize(384);
    }

    @Test
    void ingest_shouldStorePendingThreatWithNullAnalysis_whenAnalysisFails() {
        String id = "ingest-fail-" + UUID.randomUUID();
        when(groqAiClient.analyze(any(), any())).thenReturn(null);

        Optional<AnalyzedThreatEvent> result = threatIngestionService.ingest(raw(id));

        // Nothing is announced, but the threat is kept for another attempt
        assertThat(result).isEmpty();
        Threat stored = threatRepository.findByExternalId(id).orElseThrow();
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(stored.getSeverity()).isNull();
        assertThat(stored.getThreatCategory()).isNull();
        assertThat(stored.getAiSummary()).isNull();
        assertThat(stored.getAnalyzedAt()).isNull();
        assertThat(stored.getAnalysisAttemptedAt()).isNotNull();
    }

    @Test
    void ingest_shouldNotAnalyzeAgain_whenThreatAlreadyExists() {
        String id = "ingest-twice-" + UUID.randomUUID();
        when(groqAiClient.analyze(any(), any())).thenReturn(null);
        threatIngestionService.ingest(raw(id));

        // Second call: the pending threat exists, so it is skipped and not stored twice
        Optional<AnalyzedThreatEvent> second = threatIngestionService.ingest(raw(id));

        assertThat(second).isEmpty();
        assertThat(threatRepository.findAll().stream()
                .filter(t -> id.equals(t.getExternalId()))).hasSize(1);
    }
}
