package com.threatpulse.analyzer;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.FeedService;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the re-analysis against a real database. The risky parts are database behaviour that
 * mocks cannot show: the order of the queue, and saving a threat that was loaded earlier and
 * whose list of technologies was replaced. Groq and Hugging Face are mocks.
 */
public class ThreatReanalysisIntegrationTest extends BaseIntegrationTest {

    @Autowired private ThreatReanalysisService threatReanalysisService;
    @Autowired private ThreatRepository threatRepository;
    @Autowired private FeedService feedService;

    @MockitoBean private GroqAiClient groqAiClient;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private AnalyzedThreatPublisher analyzedThreatPublisher;

    @BeforeEach
    void cleanTable() {
        // Other test classes share this database, and the queue must contain only this test's data
        threatRepository.deleteAll();
    }

    private ThreatAnalysis analysis() {
        ThreatAnalysis analysis = new ThreatAnalysis();
        analysis.setSummary("A summary");
        analysis.setSeverity("CRITICAL");
        analysis.setCategory("SUPPLY_CHAIN");
        analysis.setAffectedTechnologies(List.of("npm", "node.js"));
        analysis.setRecommendedAction("Remove it");
        return analysis;
    }

    private Threat savePending(String title, OffsetDateTime attemptedAt) {
        Threat threat = new Threat();
        threat.setExternalId("reanalysis-" + title);
        threat.setTitle(title);
        threat.setDescription("Description");
        threat.setSourceUrl("https://example.com/" + title);
        threat.setSourceName("RSS");
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAnalysisStatus(AnalysisStatus.PENDING_ANALYSIS);
        threat.setAnalysisAttemptedAt(attemptedAt);
        return threatRepository.save(threat);
    }

    private Threat saveAnalyzed(String title, float[] embedding) {
        Threat threat = savePending(title, null);
        threat.setAnalysisStatus(AnalysisStatus.ANALYZED);
        threat.setSeverity(Severity.HIGH);
        threat.setAiSummary("A summary");
        threat.setAnalyzedAt(OffsetDateTime.now());
        threat.setEmbedding(embedding);
        return threatRepository.save(threat);
    }

    private Threat reload(Threat threat) {
        return threatRepository.findById(threat.getId()).orElseThrow();
    }

    @Test
    void analyzePending_shouldMakePendingThreatsAnalyzedVisibleAndComplete() {
        Threat first = savePending("First", null);
        Threat second = savePending("Second", null);
        when(groqAiClient.analyze(any(), any())).thenReturn(analysis());
        when(embeddingService.embedDocument(any())).thenReturn(new float[384]);

        int result = threatReanalysisService.analyzePending(10);

        assertThat(result).isEqualTo(2);
        for (Threat threat : List.of(first, second)) {
            Threat stored = reload(threat);
            assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
            assertThat(stored.getSeverity()).isEqualTo(Severity.CRITICAL);
            assertThat(stored.getAiSummary()).isEqualTo("A summary");
            assertThat(stored.getAnalyzedAt()).isNotNull();
            assertThat(stored.getEmbedding()).hasSize(384);
            // Read through the service, which has a session for the lazy technologies list
            assertThat(feedService.getThreatById(threat.getId()).affectedTechnologies())
                    .containsExactlyInAnyOrder("npm", "node.js");
        }
        verify(analyzedThreatPublisher, times(2)).publish(any(AnalyzedThreatEvent.class));
    }

    @Test
    void analyzePending_shouldTryNeverAttemptedFirst_thenTheLeastRecentlyAttempted() {
        OffsetDateTime now = OffsetDateTime.now();
        savePending("TwoHoursAgo", now.minusHours(2));
        savePending("NeverTried", null);
        savePending("OneHourAgo", now.minusHours(1));
        when(groqAiClient.analyze(any(), any())).thenReturn(null);

        // One threat per run, and every analysis fails: the queue must rotate
        threatReanalysisService.analyzePending(1);
        threatReanalysisService.analyzePending(1);
        threatReanalysisService.analyzePending(1);

        ArgumentCaptor<String> titles = ArgumentCaptor.forClass(String.class);
        verify(groqAiClient, times(3)).analyze(titles.capture(), any());
        assertThat(titles.getAllValues()).containsExactly("NeverTried", "TwoHoursAgo", "OneHourAgo");
    }

    @Test
    void analyzePending_shouldNotBeBlockedByAThreatThatAlwaysFails() {
        Threat poison = savePending("Poison", null);
        Threat healthy = savePending("Healthy", null);
        // Whatever the reason, this one threat can never be analyzed
        when(groqAiClient.analyze(any(), any())).thenAnswer(invocation ->
                ((String) invocation.getArgument(0)).startsWith("Poison") ? null : analysis());
        when(embeddingService.embedDocument(any())).thenReturn(null);

        int firstRun = threatReanalysisService.analyzePending(1);
        int secondRun = threatReanalysisService.analyzePending(1);

        // The first run tried the poison threat, the second run moved on to the healthy one
        assertThat(firstRun).isZero();
        assertThat(secondRun).isEqualTo(1);
        assertThat(reload(poison).getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(reload(healthy).getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
    }

    @Test
    void analyzePending_shouldKeepFailedThreatsHiddenAndWithoutMadeUpValues() {
        Threat failing = savePending("Failing", null);
        when(groqAiClient.analyze(any(), any())).thenReturn(null);

        threatReanalysisService.analyzePending(10);

        Threat stored = reload(failing);
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(stored.getSeverity()).isNull();
        assertThat(stored.getAnalysisAttemptedAt()).isNotNull();
        verify(analyzedThreatPublisher, never()).publish(any());
    }

    @Test
    void backfillEmbeddings_shouldFillAnalyzedThreatsOnly() {
        Threat withoutEmbedding = saveAnalyzed("NeedsEmbedding", null);
        Threat alreadyDone = saveAnalyzed("HasEmbedding", new float[384]);
        Threat stillPending = savePending("StillPending", null);
        float[] created = new float[384];
        created[0] = 1f;
        when(embeddingService.embedDocument(any())).thenReturn(created);

        int result = threatReanalysisService.backfillEmbeddings(10);

        assertThat(result).isEqualTo(1);
        assertThat(reload(withoutEmbedding).getEmbedding()).isEqualTo(created);
        assertThat(reload(alreadyDone).getEmbedding()).isNotEqualTo(created);
        // A pending threat is not embedded: its text will improve once it is analyzed
        assertThat(reload(stillPending).getEmbedding()).isNull();
    }
}
