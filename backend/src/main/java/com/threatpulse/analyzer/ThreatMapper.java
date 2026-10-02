package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.HashSet;

@Slf4j
@Component
public class ThreatMapper {

    /**
     * Builds a new, fully analyzed threat.
     */
    public Threat toThreat(AnalyzedThreatEvent analyzed) {
        Threat threat = new Threat();

        threat.setTitle(analyzed.title());
        threat.setDescription(analyzed.description());
        threat.setSourceName(analyzed.sourceName());
        threat.setSourceUrl(analyzed.sourceUrl());
        threat.setExternalId(analyzed.externalId());
        threat.setPublishedAt(analyzed.publishedAt());

        // One time for collected and analyzed, so both fields are consistent
        OffsetDateTime now = OffsetDateTime.now();
        threat.setCollectedAt(now);
        fillAnalysis(threat, analyzed, now);

        return threat;
    }

    /**
     * Builds a threat whose AI analysis failed. The source data is real and is kept, the analysis
     * fields stay empty (NULL), never made up, and the threat is hidden until it is analyzed.
     */
    public Threat toPendingThreat(RawThreatEvent raw) {
        Threat threat = new Threat();

        threat.setTitle(raw.title());
        threat.setDescription(raw.description());
        threat.setSourceName(raw.sourceName());
        threat.setSourceUrl(raw.sourceUrl());
        threat.setExternalId(raw.externalId());
        threat.setPublishedAt(raw.publishedAt());

        OffsetDateTime now = OffsetDateTime.now();
        threat.setCollectedAt(now);
        threat.setAnalysisAttemptedAt(now);
        threat.setAnalysisStatus(AnalysisStatus.PENDING_ANALYSIS);

        return threat;
    }

    /**
     * Writes a finished analysis into a stored threat and marks it as analyzed.
     */
    public void applyAnalysis(Threat threat, AnalyzedThreatEvent analyzed) {
        fillAnalysis(threat, analyzed, OffsetDateTime.now());
    }

    private void fillAnalysis(Threat threat, AnalyzedThreatEvent analyzed, OffsetDateTime now) {
        threat.setAiSummary(analyzed.aiSummary());
        threat.setAffectedTechnologies(analyzed.affectedTechnologies() == null
                ? new HashSet<>()
                : new HashSet<>(analyzed.affectedTechnologies()));
        threat.setSeverity(parseSeverity(analyzed.severity()));
        threat.setThreatCategory(parseCategory(analyzed.category()));
        threat.setAnalyzedAt(now);
        threat.setAnalysisAttemptedAt(now);
        threat.setAnalysisStatus(AnalysisStatus.ANALYZED);
    }

    private Severity parseSeverity(String value) {
        try {
            return Severity.valueOf(value);
        } catch (Exception e) {
            log.error("Failed to cast severity: {}", value, e);
            return Severity.INFO;
        }
    }

    private ThreatCategory parseCategory(String value) {
        try {
            return ThreatCategory.valueOf(value);
        } catch (Exception e) {
            log.error("Failed to cast category: {}", value, e);
            return ThreatCategory.OTHER;
        }
    }

    /**
     * The text that is turned into an embedding: title plus the summary, or the description
     * when there is no summary.
     */
    public static String embeddingText(AnalyzedThreatEvent analyzed) {
        return embeddingText(analyzed.title(), analyzed.aiSummary(), analyzed.description());
    }

    public static String embeddingText(Threat threat) {
        return embeddingText(threat.getTitle(), threat.getAiSummary(), threat.getDescription());
    }

    private static String embeddingText(String title, String summary, String description) {
        String body = summary != null && !summary.isBlank() ? summary : description;
        return title + "\n" + body;
    }
}
