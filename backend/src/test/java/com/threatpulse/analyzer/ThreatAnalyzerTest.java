package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Threat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ThreatAnalyzerTest {

    @Mock
    private GroqAiClient groqAiClient;

    @InjectMocks
    private ThreatAnalyzer threatAnalyzer;

    private RawThreatEvent buildRawEvent(String externalId) {
        return new RawThreatEvent(externalId, "Test title", "Test description",
                "http://example.com", "NVD", OffsetDateTime.now(), "CVE");
    }

    private ThreatAnalysis validAnalysis() {
        ThreatAnalysis aiResponse = new ThreatAnalysis();
        aiResponse.setSummary("Critical RCE in OpenSSL");
        aiResponse.setSeverity("CRITICAL");
        aiResponse.setCategory("RCE");
        aiResponse.setAffectedTechnologies(List.of("openssl", "linux"));
        aiResponse.setRecommendedAction("Patch immediately");
        return aiResponse;
    }

    @Test
    void analyze_shouldReturnEmpty_whenGroqReturnsNull() {
        // No made-up INFO event: the caller must know that the analysis is missing
        when(groqAiClient.analyze(any(), any())).thenReturn(null);

        Optional<AnalyzedThreatEvent> result = threatAnalyzer.analyze(buildRawEvent("ext-1"));

        assertThat(result).isEmpty();
    }

    @Test
    void analyze_shouldReturnEmpty_whenGroqThrowsException() {
        when(groqAiClient.analyze(any(), any())).thenThrow(new RuntimeException("Groq timeout"));

        Optional<AnalyzedThreatEvent> result = threatAnalyzer.analyze(buildRawEvent("ext-2"));

        assertThat(result).isEmpty();
    }

    @Test
    void analyze_shouldReturnEmpty_whenTheAnswerHasNoSummary() {
        ThreatAnalysis noSummary = validAnalysis();
        noSummary.setSummary("  ");
        when(groqAiClient.analyze(any(), any())).thenReturn(noSummary);

        assertThat(threatAnalyzer.analyze(buildRawEvent("ext-3"))).isEmpty();
    }

    @Test
    void analyze_shouldMapAiResponseFields_whenGroqReturnsValidResponse() {
        when(groqAiClient.analyze(any(), any())).thenReturn(validAnalysis());

        AnalyzedThreatEvent result = threatAnalyzer.analyze(buildRawEvent("CVE-2024-9999")).orElseThrow();

        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.category()).isEqualTo("RCE");
        assertThat(result.aiSummary()).isEqualTo("Critical RCE in OpenSSL");
        assertThat(result.affectedTechnologies()).containsExactlyInAnyOrder("openssl", "linux");
        assertThat(result.externalId()).isEqualTo("CVE-2024-9999");
    }

    @Test
    void analyze_shouldUseEmptyTechnologyList_whenTheAnswerLeavesItOut() {
        ThreatAnalysis noTechnologies = validAnalysis();
        noTechnologies.setAffectedTechnologies(null);
        when(groqAiClient.analyze(any(), any())).thenReturn(noTechnologies);

        AnalyzedThreatEvent result = threatAnalyzer.analyze(buildRawEvent("ext-4")).orElseThrow();

        assertThat(result.affectedTechnologies()).isEmpty();
    }

    @Test
    void analyze_shouldAnalyzeAStoredThreat_usingItsTitleAndDescription() {
        Threat stored = new Threat();
        stored.setExternalId("stored-1");
        stored.setTitle("Stored title");
        stored.setDescription("Stored description");
        stored.setSourceUrl("http://example.com/stored");
        stored.setSourceName("RSS");
        stored.setPublishedAt(OffsetDateTime.now());
        when(groqAiClient.analyze(eq("Stored title"), eq("Stored description")))
                .thenReturn(validAnalysis());

        AnalyzedThreatEvent result = threatAnalyzer.analyze(stored).orElseThrow();

        assertThat(result.externalId()).isEqualTo("stored-1");
        assertThat(result.severity()).isEqualTo("CRITICAL");
    }
}
