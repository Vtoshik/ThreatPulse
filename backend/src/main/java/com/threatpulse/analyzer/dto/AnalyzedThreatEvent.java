package com.threatpulse.analyzer.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AnalyzedThreatEvent(
        String externalId,
        String title,
        String description,
        String aiSummary,
        String severity,        // String, not enum — consumer converts it
        String category,
        List<String> affectedTechnologies,
        String recommendedAction,
        String sourceUrl,
        String sourceName,
        OffsetDateTime publishedAt
) {}
