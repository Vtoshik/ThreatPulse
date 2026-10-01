package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ThreatMapperTest {
    private final ThreatMapper threatMapper = new ThreatMapper();

    @Test
    public void toThreat_shouldFallBackToInfo_whenSeverityIsInvalid() {
        AnalyzedThreatEvent badSeverityEvent = new
                AnalyzedThreatEvent(
                "ext-3", "title", "desc", "summary",
                "UNKNOWN_SEVERITY", "OTHER", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(badSeverityEvent);

        assertThat(threat.getSeverity()).isEqualTo(Severity.INFO);
    }

    @Test
    public void toThreat_shouldFallBackToOther_whenCategoryIsInvalid() {
        AnalyzedThreatEvent badCategoryEvent = new AnalyzedThreatEvent(
                "ext-4", "title", "desc", "summary",
                "HIGH", "UNKNOWN_CATEGORY", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(badCategoryEvent);

        assertThat(threat.getThreatCategory()).isEqualTo(ThreatCategory.OTHER);
    }

    @Test
    public void toThreat_shouldKeepSupplyChainCategory_whenAiReturnsIt() {
        // The AI prompt offers SUPPLY_CHAIN, so it must not be treated as an unknown value
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-7", "title", "desc", "summary",
                "HIGH", "SUPPLY_CHAIN", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(event);

        assertThat(threat.getThreatCategory()).isEqualTo(ThreatCategory.SUPPLY_CHAIN);
    }

    @Test
    public void embeddingText_shouldUseAiSummary_whenPresent() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-5", "title", "description", "summary",
                "HIGH", "OTHER", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        String text = ThreatMapper.embeddingText(event);

        assertThat(text).isEqualTo("title\nsummary");
    }

    @Test
    public void embeddingText_shouldFallBackToDescription_whenAiSummaryIsNull() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-6", "title", "description", null,
                "HIGH", "OTHER", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        String text = ThreatMapper.embeddingText(event);

        assertThat(text).isEqualTo("title\ndescription");
    }
}