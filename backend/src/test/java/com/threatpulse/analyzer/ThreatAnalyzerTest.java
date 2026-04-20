package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.collector.dto.RawThreatEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void analyze_shouldReturnDefaultEvent_whenGroqReturnsNull() {
        when(groqAiClient.analyze(any(), any())).thenReturn(null);

        AnalyzedThreatEvent result = threatAnalyzer.analyze(buildRawEvent("ext-1"));

        assertThat(result).isNotNull();
        assertThat(result.severity()).isEqualTo("INFO");
        assertThat(result.category()).isEqualTo("OTHER");
        assertThat(result.externalId()).isEqualTo("ext-1");
        assertThat(result.affectedTechnologies()).isEmpty();
    }

    @Test
    void analyze_shouldReturnDefaultEvent_whenGroqThrowsException() {
        // Groq API is unreachable or returns unparseable response
        when(groqAiClient.analyze(any(), any())).thenThrow(new RuntimeException("Groq timeout"));

        AnalyzedThreatEvent result = threatAnalyzer.analyze(buildRawEvent("ext-2"));

        // Pipeline must not break — default values used
        assertThat(result.severity()).isEqualTo("INFO");
        assertThat(result.externalId()).isEqualTo("ext-2");
    }

    @Test
    void analyze_shouldMapAiResponseFields_whenGroqReturnsValidResponse() {
        ThreatAnalysis aiResponse = new ThreatAnalysis();
        aiResponse.setSummary("Critical RCE in OpenSSL");
        aiResponse.setSeverity("CRITICAL");
        aiResponse.setCategory("RCE");
        aiResponse.setAffectedTechnologies(List.of("openssl", "linux"));
        aiResponse.setRecommendedAction("Patch immediately");

        when(groqAiClient.analyze(any(), any())).thenReturn(aiResponse);

        AnalyzedThreatEvent result = threatAnalyzer.analyze(buildRawEvent("CVE-2024-9999"));

        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.category()).isEqualTo("RCE");
        assertThat(result.aiSummary()).isEqualTo("Critical RCE in OpenSSL");
        assertThat(result.affectedTechnologies()).containsExactlyInAnyOrder("openssl", "linux");
        assertThat(result.externalId()).isEqualTo("CVE-2024-9999");
    }
}