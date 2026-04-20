package com.threatpulse.feed.dto;

import java.util.List;

/**
 * DTO representing a paginated response of threats.
 * <p>
 * Contains a list of threats along with pagination metadata such as
 * current page, page size, total elements, and total pages.
 */
public record ThreatPageResponse(
        List<ThreatResponse> threats,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
