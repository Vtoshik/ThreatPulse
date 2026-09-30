package com.threatpulse.feed.dto;

import java.util.List;

public record SemanticSearchResponse(
        List<ThreatResponse> threats
) {}
