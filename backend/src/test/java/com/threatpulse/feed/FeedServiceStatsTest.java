package com.threatpulse.feed;

import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.feed.dto.ThreatStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FeedServiceStatsTest {
    @Mock private ThreatRepository threatRepository;
    @Mock private ThreatResponseMapper threatResponseMapper;

    @InjectMocks private FeedService feedService;

    @Test
    void getStats_shouldMapEachSeverityAndSumTheTotal() {
        when(threatRepository.countAnalyzedBySeverity()).thenReturn(List.of(
                new Object[]{Severity.CRITICAL, 3L},
                new Object[]{Severity.HIGH, 5L},
                new Object[]{Severity.LOW, 2L}));
        when(threatRepository.countByAnalysisStatusAndPublishedAtAfter(
                eq(AnalysisStatus.ANALYZED), any(OffsetDateTime.class))).thenReturn(4L);

        ThreatStatsResponse stats = feedService.getStats();

        assertThat(stats.critical()).isEqualTo(3);
        assertThat(stats.high()).isEqualTo(5);
        assertThat(stats.medium()).isZero();
        assertThat(stats.low()).isEqualTo(2);
        assertThat(stats.info()).isZero();
        assertThat(stats.total()).isEqualTo(10);
        assertThat(stats.last24h()).isEqualTo(4);
    }

    @Test
    void getStats_shouldReturnZeros_whenThereAreNoThreats() {
        when(threatRepository.countAnalyzedBySeverity()).thenReturn(List.of());
        when(threatRepository.countByAnalysisStatusAndPublishedAtAfter(
                eq(AnalysisStatus.ANALYZED), any(OffsetDateTime.class))).thenReturn(0L);

        ThreatStatsResponse stats = feedService.getStats();

        assertThat(stats.total()).isZero();
    }

    @Test
    void getStats_shouldIgnoreRowsWithoutSeverity() {
        when(threatRepository.countAnalyzedBySeverity()).thenReturn(
                Arrays.<Object[]>asList(new Object[]{null, 9L}, new Object[]{Severity.INFO, 1L}));
        when(threatRepository.countByAnalysisStatusAndPublishedAtAfter(
                eq(AnalysisStatus.ANALYZED), any(OffsetDateTime.class))).thenReturn(0L);

        assertThat(feedService.getStats().total()).isEqualTo(1);
    }
}
