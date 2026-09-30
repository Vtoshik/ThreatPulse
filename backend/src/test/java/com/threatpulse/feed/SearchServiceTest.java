package com.threatpulse.feed;

import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.dto.SemanticSearchResponse;
import com.threatpulse.feed.dto.ThreatPageResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
    private static final float[] QUERY_VECTOR = {0.5f, 0.25f};
    // The text form pgvector receives: Arrays.toString of the vector above
    private static final String QUERY_VECTOR_TEXT = "[0.5, 0.25]";

    @Mock private EmbeddingService embeddingService;
    @Mock private ThreatRepository threatRepository;
    @Mock private ThreatResponseMapper threatResponseMapper;
    @Mock private FeedService feedService;

    @InjectMocks
    private SearchService searchService;

    private ThreatResponse buildResponse(String externalId) {
        return new ThreatResponse(1L, externalId, "title", "desc", "summary",
                Severity.HIGH, ThreatCategory.OTHER, "NVD", "https://example.com",
                OffsetDateTime.now(), Set.of("spring-boot"));
    }

    @Test
    void search_shouldUseVectorSearch_whenQueryCanBeEmbedded() {
        Threat threat = new Threat();
        ThreatResponse response = buildResponse("ext-1");
        when(embeddingService.embedQuery("spring rce")).thenReturn(QUERY_VECTOR);
        when(threatRepository.findNearest(QUERY_VECTOR_TEXT, "HIGH", 5)).thenReturn(List.of(threat));
        when(threatResponseMapper.toThreatResponse(threat)).thenReturn(response);

        SemanticSearchResponse result = searchService.search("spring rce", Severity.HIGH, 5);

        assertThat(result.threats()).containsExactly(response);
        // The keyword feed must not be used when the vector search works
        verifyNoInteractions(feedService);
    }

    @Test
    void search_shouldPassNullSeverityToRepository_whenNoSeverityIsGiven() {
        when(embeddingService.embedQuery("spring rce")).thenReturn(QUERY_VECTOR);
        when(threatRepository.findNearest(QUERY_VECTOR_TEXT, null, 5)).thenReturn(List.of());

        SemanticSearchResponse result = searchService.search("spring rce", null, 5);

        assertThat(result.threats()).isEmpty();
        verify(threatRepository).findNearest(QUERY_VECTOR_TEXT, null, 5);
    }

    @Test
    void search_shouldClampLimitToMaximum_whenLimitIsTooLarge() {
        when(embeddingService.embedQuery("spring rce")).thenReturn(QUERY_VECTOR);
        when(threatRepository.findNearest(QUERY_VECTOR_TEXT, null, 50)).thenReturn(List.of());

        searchService.search("spring rce", null, 100_000);

        verify(threatRepository).findNearest(QUERY_VECTOR_TEXT, null, 50);
    }

    @Test
    void search_shouldClampLimitToOne_whenLimitIsZeroOrNegative() {
        when(embeddingService.embedQuery("spring rce")).thenReturn(QUERY_VECTOR);
        when(threatRepository.findNearest(QUERY_VECTOR_TEXT, null, 1)).thenReturn(List.of());

        searchService.search("spring rce", null, 0);
        searchService.search("spring rce", null, -5);

        // A LIMIT of 0 or less would return nothing or fail in SQL
        verify(threatRepository, times(2)).findNearest(QUERY_VECTOR_TEXT, null, 1);
    }

    @Test
    void search_shouldTruncateLongQuery_beforeCallingTheEmbeddingApi() {
        String longQuery = "a".repeat(500);
        String truncated = "a".repeat(200);
        when(embeddingService.embedQuery(truncated)).thenReturn(QUERY_VECTOR);
        when(threatRepository.findNearest(QUERY_VECTOR_TEXT, null, 10)).thenReturn(List.of());

        searchService.search(longQuery, null, 10);

        // The stub only matches the truncated text, so an untruncated call would return null
        // and take the fallback path, which this verification would then reveal
        verify(threatRepository).findNearest(QUERY_VECTOR_TEXT, null, 10);
        verifyNoInteractions(feedService);
    }

    @Test
    void search_shouldUseTruncatedQueryAndClampedLimit_inTheKeywordFallback() {
        String longQuery = "b".repeat(500);
        String truncated = "b".repeat(200);
        when(embeddingService.embedQuery(truncated)).thenReturn(null);
        when(feedService.getThreats(0, 50, null, truncated))
                .thenReturn(new ThreatPageResponse(List.of(), 0, 50, 0, 0));

        SemanticSearchResponse result = searchService.search(longQuery, null, 100_000);

        assertThat(result.threats()).isEmpty();
        verify(feedService).getThreats(0, 50, null, truncated);
    }

    @Test
    void search_shouldFallBackToKeywordSearchWithQueryAndSeverity_whenEmbeddingIsUnavailable() {
        ThreatResponse response = buildResponse("ext-2");
        when(embeddingService.embedQuery("spring rce")).thenReturn(null);
        when(feedService.getThreats(0, 5, Severity.CRITICAL, "spring rce"))
                .thenReturn(new ThreatPageResponse(List.of(response), 0, 5, 1, 1));

        SemanticSearchResponse result = searchService.search("spring rce", Severity.CRITICAL, 5);

        assertThat(result.threats()).containsExactly(response);
        // The vector query must not run without a vector
        verify(threatRepository, never()).findNearest(any(), any(), anyInt());
    }
}
