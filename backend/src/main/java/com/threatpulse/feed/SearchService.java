package com.threatpulse.feed;

import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.common.domain.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SerchService {
    private final EmbeddingService embeddingService;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;
    private final FeedService feedService;

    public SemanticSearchResponse search(String query, Severity severity, int limit) {
        float[] queryVector = embeddingService.embedQuery(query);
        if (queryVector == null) {
            // fallback: what does calling the existing keyword path look like here,
            // and how do you fit its Page-shaped result into a SemanticSearchResponse?
        }
        // turn queryVector into pgvector's text form — where did that formatting logic end up living?
        // call findNearest, map each Threat, wrap in SemanticSearchResponse
    }
}
