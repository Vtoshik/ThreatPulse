package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.AnalysisStatus;
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

    @Test
    public void embeddingText_shouldFallBackToDescription_whenAiSummaryIsBlank() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-8", "title", "description", "   ",
                "HIGH", "OTHER", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        assertThat(ThreatMapper.embeddingText(event)).isEqualTo("title\ndescription");
    }

    @Test
    public void embeddingText_shouldWorkForAStoredThreat() {
        Threat withSummary = new Threat();
        withSummary.setTitle("title");
        withSummary.setDescription("description");
        withSummary.setAiSummary("summary");
        Threat withoutSummary = new Threat();
        withoutSummary.setTitle("title");
        withoutSummary.setDescription("description");

        assertThat(ThreatMapper.embeddingText(withSummary)).isEqualTo("title\nsummary");
        assertThat(ThreatMapper.embeddingText(withoutSummary)).isEqualTo("title\ndescription");
    }

    @Test
    public void toThreat_shouldBeAnalyzedWithOneTimeForCollectedAndAnalyzed() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-9", "title", "desc", "summary",
                "HIGH", "RCE", List.of("redis"),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(event);

        assertThat(threat.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(threat.getAnalyzedAt()).isEqualTo(threat.getCollectedAt());
        assertThat(threat.getAnalysisAttemptedAt()).isEqualTo(threat.getAnalyzedAt());
    }

    @Test
    public void toThreat_shouldUseEmptyTechnologies_whenTheEventHasNone() {
        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-10", "title", "desc", "summary",
                "HIGH", "RCE", null,
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(event);

        assertThat(threat.getAffectedTechnologies()).isEmpty();
    }

    @Test
    public void toPendingThreat_shouldKeepSourceDataAndLeaveAnalysisEmpty() {
        OffsetDateTime published = OffsetDateTime.now().minusDays(1);
        RawThreatEvent raw = new RawThreatEvent("ext-11", "Raw title", "Raw description",
                "https://example.com/11", "RSS", published, "RSS");

        Threat threat = threatMapper.toPendingThreat(raw);

        assertThat(threat.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_ANALYSIS);
        assertThat(threat.getExternalId()).isEqualTo("ext-11");
        assertThat(threat.getTitle()).isEqualTo("Raw title");
        assertThat(threat.getDescription()).isEqualTo("Raw description");
        assertThat(threat.getSourceUrl()).isEqualTo("https://example.com/11");
        assertThat(threat.getSourceName()).isEqualTo("RSS");
        assertThat(threat.getPublishedAt()).isEqualTo(published);
        assertThat(threat.getCollectedAt()).isNotNull();
        assertThat(threat.getAnalysisAttemptedAt()).isNotNull();
        // Unknown values stay NULL, they are never made up
        assertThat(threat.getSeverity()).isNull();
        assertThat(threat.getThreatCategory()).isNull();
        assertThat(threat.getAiSummary()).isNull();
        assertThat(threat.getAnalyzedAt()).isNull();
        assertThat(threat.getAffectedTechnologies()).isEmpty();
    }

    @Test
    public void applyAnalysis_shouldFillAPendingThreatAndMarkItAnalyzed() {
        Threat pending = threatMapper.toPendingThreat(new RawThreatEvent("ext-12", "title",
                "desc", "https://example.com", "RSS", OffsetDateTime.now(), "RSS"));
        AnalyzedThreatEvent analysis = new AnalyzedThreatEvent(
                "ext-12", "title", "desc", "A new summary",
                "CRITICAL", "SUPPLY_CHAIN", List.of("npm"),
                "action", "https://example.com", "RSS",
                OffsetDateTime.now()
        );

        threatMapper.applyAnalysis(pending, analysis);

        assertThat(pending.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(pending.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(pending.getThreatCategory()).isEqualTo(ThreatCategory.SUPPLY_CHAIN);
        assertThat(pending.getAiSummary()).isEqualTo("A new summary");
        assertThat(pending.getAffectedTechnologies()).containsExactly("npm");
        assertThat(pending.getAnalyzedAt()).isNotNull();
    }
}