package com.threatpulse.feed;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.bookmarks.Bookmark;
import com.threatpulse.bookmarks.BookmarkRepository;
import com.threatpulse.bookmarks.BookmarkService;
import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.common.exception.ResourceNotFoundException;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.feed.dto.ThreatPageResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A threat that is waiting for its AI analysis has unknown severity, category and summary.
 * Every way of reading threats must ignore it. The pending threat here has the same embedding
 * as the search query, so a missing filter would put it first in the semantic search.
 */
public class PendingThreatVisibilityIntegrationTest extends BaseIntegrationTest {

    @Autowired private ThreatRepository threatRepository;
    @Autowired private FeedService feedService;
    @Autowired private SearchService searchService;
    @Autowired private BookmarkService bookmarkService;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private EmbeddingService embeddingService;

    private String keyword;
    private Threat analyzed;
    private Threat pending;
    private User user;

    private static float[] vector() {
        float[] vector = new float[384];
        vector[0] = 1f;
        return vector;
    }

    private Threat save(String title, AnalysisStatus status) {
        Threat threat = new Threat();
        threat.setExternalId("visibility-" + UUID.randomUUID());
        threat.setTitle(title);
        threat.setDescription("Description");
        threat.setSourceUrl("https://example.com");
        threat.setSourceName("TEST");
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setEmbedding(vector());
        threat.setAnalysisStatus(status);
        if (status == AnalysisStatus.ANALYZED) {
            threat.setSeverity(Severity.HIGH);
            threat.setThreatCategory(ThreatCategory.RCE);
            threat.setAiSummary("A summary");
            threat.setAnalyzedAt(OffsetDateTime.now());
        }
        return threatRepository.save(threat);
    }

    @BeforeEach
    void setUp() {
        // A new keyword per test keeps cached feed pages from one test out of another
        keyword = "kw" + UUID.randomUUID().toString().replace("-", "");
        analyzed = save("Analyzed " + keyword, AnalysisStatus.ANALYZED);
        pending = save("Pending " + keyword, AnalysisStatus.PENDING_ANALYSIS);

        String unique = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(new User("user-" + unique, unique + "@example.com", "hash"));
    }

    private static List<Long> ids(List<ThreatResponse> threats) {
        return threats.stream().map(ThreatResponse::id).toList();
    }

    @Test
    void feed_shouldNotListPendingThreats() {
        ThreatPageResponse page = feedService.getThreats(0, 50, null, keyword);

        assertThat(ids(page.threats())).containsExactly(analyzed.getId());
    }

    @Test
    void threatById_shouldTreatAPendingThreatAsNotFound() {
        assertThat(feedService.getThreatById(analyzed.getId()).id()).isEqualTo(analyzed.getId());

        assertThatThrownBy(() -> feedService.getThreatById(pending.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nearestSearch_shouldNotReturnPendingThreats() {
        List<Threat> result = threatRepository.findNearest(Arrays.toString(vector()), null, 50);

        assertThat(result).extracting(Threat::getId).contains(analyzed.getId())
                .doesNotContain(pending.getId());
    }

    @Test
    void semanticSearch_shouldNotReturnPendingThreats() {
        when(embeddingService.embedQuery(any())).thenReturn(vector());

        List<ThreatResponse> result = searchService.search(keyword, null, 50).threats();

        assertThat(ids(result)).contains(analyzed.getId()).doesNotContain(pending.getId());
    }

    @Test
    void keywordFallback_shouldNotReturnPendingThreats() {
        // No query embedding is available, so the search falls back to the keyword feed
        when(embeddingService.embedQuery(any())).thenReturn(null);

        List<ThreatResponse> result = searchService.search(keyword, null, 50).threats();

        assertThat(ids(result)).containsExactly(analyzed.getId());
    }

    @Test
    void bookmarks_shouldRefusePendingThreats() {
        assertThatThrownBy(() -> bookmarkService.addBookmark(user, pending.getId()))
                .isInstanceOf(ThreatPulseException.class);

        bookmarkService.addBookmark(user, analyzed.getId());
        assertThat(ids(bookmarkService.getBookmarks(user))).containsExactly(analyzed.getId());
    }

    @Test
    void bookmarks_shouldNotListAPendingThreatThatWasBookmarkedBefore() {
        // For example a bookmark that existed before the threat was marked as pending
        bookmarkRepository.save(new Bookmark(user.getId(), pending.getId()));
        bookmarkRepository.save(new Bookmark(user.getId(), analyzed.getId()));

        assertThat(ids(bookmarkService.getBookmarks(user))).containsExactly(analyzed.getId());
    }

    @Test
    void alertQuery_shouldOnlyReturnAnalyzedThreats() {
        // Even a pending threat with an analysis time (an inconsistent row) is not returned
        pending.setAnalyzedAt(OffsetDateTime.now());
        threatRepository.save(pending);

        List<Threat> result = threatRepository.findByAnalysisStatusAndAnalyzedAtAfter(
                AnalysisStatus.ANALYZED, OffsetDateTime.now().minusDays(1));

        assertThat(result).extracting(Threat::getId).contains(analyzed.getId())
                .doesNotContain(pending.getId());
    }
}
