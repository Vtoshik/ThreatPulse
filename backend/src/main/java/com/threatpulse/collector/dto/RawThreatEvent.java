package com.threatpulse.collector.dto;

import java.time.OffsetDateTime;

public record RawThreatEvent (
        String externalId,
        String title,
        String description,
        String sourceUrl,
        String sourceName,
        OffsetDateTime publishedAt,
        String rawType  // "CVE", "NEWS", "RSS"
) {}
