package com.threatpulse.feed;

import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.dto.SemanticSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class SearchService {
    private final EmbeddingService embeddingService;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;
    private final FeedService feedService;
    private final TransactionTemplate readOnlyTransaction;

    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_CHARS = 200;

    public SearchService(EmbeddingService embeddingService,
                         ThreatRepository threatRepository,
                         ThreatResponseMapper threatResponseMapper,
                         FeedService feedService,
                         PlatformTransactionManager transactionManager) {
        this.embeddingService = embeddingService;
        this.threatRepository = threatRepository;
        this.threatResponseMapper = threatResponseMapper;
        this.feedService = feedService;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    /**
     * Semantic search with a keyword fallback.
     * <p>
     * The method is deliberately not @Transactional: the call to the embedding API can
     * take seconds, and a transaction around it would hold a database connection the
     * whole time. Only the database work runs inside a short read-only transaction.
     */
    public SemanticSearchResponse search(String query, Severity severity, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String safeQuery = query.substring(0, Math.min(query.length(), MAX_QUERY_CHARS));

        float[] queryVector = embeddingService.embedQuery(safeQuery);
        if (queryVector == null) {
            return keywordSearch(safeQuery, severity, safeLimit);
        }

        String vectorText = Arrays.toString(queryVector);
        SemanticSearchResponse semantic = readOnlyTransaction.execute(status -> {
            List<Threat> nearest = threatRepository.findNearest(vectorText,
                    severity != null ? severity.name() : null, safeLimit);
            return new SemanticSearchResponse(nearest.stream()
                    .map(threatResponseMapper::toThreatResponse).toList());
        });

        // No row has a vector yet (fresh deploy, backfill still running): keyword search
        // is better than an empty answer
        if (semantic == null || semantic.threats().isEmpty()) {
            return keywordSearch(safeQuery, severity, safeLimit);
        }
        return semantic;
    }

    private SemanticSearchResponse keywordSearch(String query, Severity severity, int limit) {
        return new SemanticSearchResponse(
                feedService.getThreats(0, limit, severity, query).threats());
    }
}
