package com.threatpulse.feed;

import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.dto.SemanticSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final EmbeddingService embeddingService;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;
    private final FeedService feedService;

    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_CHARS = 200;

    @Transactional(readOnly = true)
    public SemanticSearchResponse search(String query, Severity severity, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String safeQuery = query.substring(0, Math.min(query.length(), MAX_QUERY_CHARS));

        float[] queryVector = embeddingService.embedQuery(safeQuery);
        if (queryVector == null) {
            return new SemanticSearchResponse(feedService.getThreats(0, safeLimit,
                    severity, safeQuery).threats());
        }
        String vectorText = Arrays.toString(queryVector);
        List<Threat> nearest = threatRepository.findNearest(vectorText,
                severity!= null ? severity.name() : null, safeLimit);
        return new SemanticSearchResponse(nearest.stream()
                .map(threatResponseMapper::toThreatResponse).toList());
    }
}
