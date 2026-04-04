package com.threatpulse.feed.dto;

import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.ThreatCategory;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO representing a single threat returned to the client.
 * <p>
 * Contains summarized threat information used for display in feeds or lists,
 * excluding sensitive or internal fields.
 */
public record ThreatResponse (
    Long id,
    String externalId,
    String title,
    String aiSummary,
    Severity severity,
    ThreatCategory threatCategory,
    String sourceName,
    String sourceUrl,
    OffsetDateTime publishedAt,
    Set<String> affectedTechnologies
) {}
