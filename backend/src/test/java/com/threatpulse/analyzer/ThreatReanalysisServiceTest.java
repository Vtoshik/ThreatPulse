package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Uses the real ThreatMapper, so what is saved can be inspected, and mocks the database and
 * the external APIs.
 */
@ExtendWith(MockitoExtension.class)
public class ThreatReanalysisServiceTest {

    @Mock private ThreatRepository threatRepository;
    @Mock private ThreatAnalyzer threatAnalyzer;
    @Mock private EmbeddingService embeddingService;
    @Mock private AnalyzedThreatPublisher analyzedThreatPublisher;

    private final ThreatMapper threatMapper = new ThreatMapper();

    private ThreatReanalysisService service() {
        return new ThreatReanalysisService(threatRepository, threatAnalyzer, threatMapper,
                embeddingService, analyzedThreatPublisher);
    }

    private Threat pendingThreat(String externalId) {
        Threat threat = new Threat();
        threat.setExternalId(externalId);
        threat.setTitle("Title " + externalId);
        threat.setDescription("Description");
        threat.setSourceUrl("https://example.com");
        threat.setSourceName("RSS");
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAnalysisStatus(AnalysisStatus.PENDING_ANALYSIS);
        return threat;
    }

    private AnalyzedThreatEvent analysisOf(Threat threat) {
        return new AnalyzedThreatEvent(threat.getExternalId(), threat.getTitle(),
                threat.getDescription(), "A summary", "CRITICAL", "SUPPLY_CHAIN",
                List.of("npm"), "Remove it", threat.getSourceUrl(), threat.getSourceName(),
                threat.getPublishedAt());
    }

    @Test
    void analyzePending_shouldDoNothing_whenNothingIsPending() {
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of());

        int result = service().analyzePending(20);

        assertThat(result).isZero();
        verifyNoInteractions(threatAnalyzer, embeddingService, analyzedThreatPublisher);
    }

    @Test
    void analyzePending_shouldAskForOnlyOneBatch() {
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of());

        service().analyzePending(7);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(threatRepository).findPendingForAnalysis(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(7);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void analyzePending_shouldFillMarkAndPublish_whenAnalysisWorks() {
        float[] embedding = new float[]{0.5f};
        Threat threat = pendingThreat("t-1");
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of(threat));
        when(threatAnalyzer.analyze(threat)).thenReturn(Optional.of(analysisOf(threat)));
        when(embeddingService.embedDocument("Title t-1\nA summary")).thenReturn(embedding);

        int result = service().analyzePending(20);

        assertThat(result).isEqualTo(1);
        assertThat(threat.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(threat.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(threat.getThreatCategory()).isEqualTo(ThreatCategory.SUPPLY_CHAIN);
        assertThat(threat.getAiSummary()).isEqualTo("A summary");
        assertThat(threat.getAffectedTechnologies()).containsExactly("npm");
        assertThat(threat.getAnalyzedAt()).isNotNull();
        assertThat(threat.getEmbedding()).isEqualTo(embedding);
        verify(threatRepository).save(threat);
        verify(analyzedThreatPublisher).publish(any(AnalyzedThreatEvent.class));
    }

    @Test
    void analyzePending_shouldStillAnalyze_whenTheEmbeddingFails() {
        // The backfill creates the embedding later, the analysis must not wait for it
        Threat threat = pendingThreat("t-2");
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of(threat));
        when(threatAnalyzer.analyze(threat)).thenReturn(Optional.of(analysisOf(threat)));
        when(embeddingService.embedDocument(any())).thenReturn(null);

        int result = service().analyzePending(20);

        assertThat(result).isEqualTo(1);
        assertThat(threat.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(threat.getEmbedding()).isNull();
        verify(analyzedThreatPublisher).publish(any(AnalyzedThreatEvent.class));
    }

    @Test
    void analyzePending_shouldKeepThreatPendingAndRecordTheAttempt_whenAnalysisFails() {
        Threat threat = pendingThreat("t-3");
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of(threat));
        when(threatAnalyzer.analyze(threat)).thenReturn(Optional.empty());

        int result = service().analyzePending(20);

        assertThat(result).isZero();
        assertThat(threat.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(threat.getSeverity()).isNull();
        // The attempt time moves the threat to the back of the queue
        assertThat(threat.getAnalysisAttemptedAt()).isNotNull();
        verify(threatRepository).save(threat);
        verifyNoInteractions(embeddingService, analyzedThreatPublisher);
    }

    @Test
    void analyzePending_shouldStopAfterThreeFailuresInARow() {
        List<Threat> threats = List.of(pendingThreat("a"), pendingThreat("b"), pendingThreat("c"),
                pendingThreat("d"), pendingThreat("e"));
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(threats);
        when(threatAnalyzer.analyze(any(Threat.class))).thenReturn(Optional.empty());

        int result = service().analyzePending(20);

        // Groq is probably limited or down: no reason to burn calls on the other two
        assertThat(result).isZero();
        verify(threatAnalyzer, times(ThreatReanalysisService.MAX_CONSECUTIVE_FAILURES))
                .analyze(any(Threat.class));
    }

    @Test
    void analyzePending_shouldKeepGoing_whenASingleThreatFailsBetweenSuccesses() {
        Threat first = pendingThreat("ok-1");
        Threat poison = pendingThreat("poison");
        Threat last = pendingThreat("ok-2");
        when(threatRepository.findPendingForAnalysis(any())).thenReturn(List.of(first, poison, last));
        when(threatAnalyzer.analyze(first)).thenReturn(Optional.of(analysisOf(first)));
        when(threatAnalyzer.analyze(poison)).thenReturn(Optional.empty());
        when(threatAnalyzer.analyze(last)).thenReturn(Optional.of(analysisOf(last)));

        int result = service().analyzePending(20);

        // One bad threat does not stop the others
        assertThat(result).isEqualTo(2);
        assertThat(poison.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(last.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
    }

    @Test
    void backfillEmbeddings_shouldFillEveryThreatInTheBatch() {
        float[] embedding = new float[]{0.1f};
        Threat first = pendingThreat("e-1");
        first.setAnalysisStatus(AnalysisStatus.ANALYZED);
        Threat second = pendingThreat("e-2");
        second.setAnalysisStatus(AnalysisStatus.ANALYZED);
        when(threatRepository.findAnalyzedWithoutEmbedding(any())).thenReturn(List.of(first, second));
        when(embeddingService.embedDocument(any())).thenReturn(embedding);

        int result = service().backfillEmbeddings(20);

        assertThat(result).isEqualTo(2);
        assertThat(first.getEmbedding()).isEqualTo(embedding);
        assertThat(second.getEmbedding()).isEqualTo(embedding);
        verify(threatRepository).save(first);
        verify(threatRepository).save(second);
    }

    @Test
    void backfillEmbeddings_shouldStopAtTheFirstFailure() {
        Threat first = pendingThreat("e-3");
        Threat second = pendingThreat("e-4");
        when(threatRepository.findAnalyzedWithoutEmbedding(any())).thenReturn(List.of(first, second));
        when(embeddingService.embedDocument(any())).thenReturn(null);

        int result = service().backfillEmbeddings(20);

        // The embedding service is probably down or out of credit: stop instead of retrying
        assertThat(result).isZero();
        verify(embeddingService, times(1)).embedDocument(any());
        verify(threatRepository, never()).save(any());
    }
}
