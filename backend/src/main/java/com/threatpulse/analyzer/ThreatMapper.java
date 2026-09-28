package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
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

    public Threat toThreat(AnalyzedThreatEvent analyzed) {
        Threat threat = new Threat();

        threat.setAffectedTechnologies(new HashSet<>(analyzed.affectedTechnologies()));
        threat.setTitle(analyzed.title());
        threat.setDescription(analyzed.description());
        threat.setSourceName(analyzed.sourceName());
        threat.setSourceUrl(analyzed.sourceUrl());
        threat.setAiSummary(analyzed.aiSummary());
        threat.setExternalId(analyzed.externalId());
        OffsetDateTime now = OffsetDateTime.now();
        threat.setCollectedAt(now);
        threat.setAnalyzedAt(now);
        threat.setPublishedAt(analyzed.publishedAt());

        Severity severity;
        try {
            severity = Severity.valueOf(analyzed.severity());
        } catch (Exception e) {
            log.error("Failed to cast severity: {}", analyzed.severity(), e);
            severity = Severity.INFO;
        }

        ThreatCategory category;
        try {
            category = ThreatCategory.valueOf(analyzed.category());
        } catch (Exception e) {
            log.error("Failed to cast category: {}", analyzed.category(), e);
            category = ThreatCategory.OTHER;
        }

        threat.setThreatCategory(category);
        threat.setSeverity(severity);

        return threat;
    }

    public static String embeddingText(AnalyzedThreatEvent analyzed) {
        String summaryOrDescription = analyzed.aiSummary() != null
                ? analyzed.aiSummary()
                : analyzed.description();
        return analyzed.title() + "\n" + summaryOrDescription;
    }
 }
