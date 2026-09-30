package com.threatpulse.feed;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.analyzer.EmbeddingService;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.dto.SemanticSearchResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Calls GET /api/threats/search through the whole stack: security filter, controller,
 * service, native pgvector query and JSON serialization, against a real database.
 * <p>
 * Only the Hugging Face call is replaced by a mock, so the query vector is known.
 * The database is shared with other test classes and the collector may add threats to it,
 * so every assertion looks only at threats created by this test (identified by a unique prefix).
 */
public class SearchEndpointIntegrationTest extends BaseIntegrationTest {
    private static final int DIMENSIONS = 384;
    private static final float[] QUERY_VECTOR = vector(1f);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ThreatRepository threatRepository;

    @MockitoBean
    private EmbeddingService embeddingService;

    private String prefix;
    private String token;

    @BeforeEach
    void setUp() {
        prefix = "search-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        token = registerAndGetToken();
        when(embeddingService.embedQuery(anyString())).thenReturn(QUERY_VECTOR);
    }

    private static float[] vector(float... leadingValues) {
        float[] vector = new float[DIMENSIONS];
        System.arraycopy(leadingValues, 0, vector, 0, leadingValues.length);
        return vector;
    }

    private String registerAndGetToken() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user-" + unique);
        request.setEmail(unique + "@example.com");
        request.setPassword("password123");
        return restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class)
                .getBody().getAccessToken();
    }

    private void saveThreat(String name, float[] embedding, Severity severity, Set<String> technologies) {
        Threat threat = new Threat();
        threat.setExternalId(prefix + name);
        threat.setTitle("Title " + prefix + name);
        threat.setDescription("Description " + name);
        threat.setSourceUrl("https://example.com/" + name);
        threat.setSourceName("TEST");
        threat.setSeverity(severity);
        threat.setThreatCategory(ThreatCategory.OTHER);
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAffectedTechnologies(technologies);
        threat.setEmbedding(embedding);
        threatRepository.save(threat);
    }

    private ResponseEntity<SemanticSearchResponse> search(String queryString, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange("/api/threats/search?" + queryString, HttpMethod.GET,
                new HttpEntity<>(headers), SemanticSearchResponse.class);
    }

    /** Names (without the unique prefix) of the threats created by this test, in result order. */
    private List<String> ownNames(ResponseEntity<SemanticSearchResponse> response) {
        return response.getBody().threats().stream()
                .map(ThreatResponse::externalId)
                .filter(id -> id.startsWith(prefix))
                .map(id -> id.substring(prefix.length()))
                .toList();
    }

    @Test
    void search_shouldReturnNearestThreatsFirst_withTechnologies() {
        saveThreat("far", vector(0f, 1f), Severity.HIGH, Set.of());
        saveThreat("identical", vector(1f), Severity.HIGH, Set.of("spring-boot", "redis"));
        saveThreat("near", vector(0.9f, 0.1f), Severity.HIGH, Set.of());
        saveThreat("no-embedding", null, Severity.HIGH, Set.of());

        ResponseEntity<SemanticSearchResponse> response = search("q=spring&limit=50", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownNames(response)).containsExactly("identical", "near", "far");
        // The technologies are a lazy collection: reading them proves the session was open
        ThreatResponse identical = response.getBody().threats().stream()
                .filter(t -> t.externalId().equals(prefix + "identical")).findFirst().orElseThrow();
        assertThat(identical.affectedTechnologies()).containsExactlyInAnyOrder("spring-boot", "redis");
    }

    @Test
    void search_shouldOnlyReturnRequestedSeverity() {
        saveThreat("high", vector(1f), Severity.HIGH, Set.of());
        saveThreat("critical", vector(0.9f, 0.1f), Severity.CRITICAL, Set.of());

        ResponseEntity<SemanticSearchResponse> response =
                search("q=spring&limit=50&severity=CRITICAL", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownNames(response)).containsExactly("critical");
    }

    @Test
    void search_shouldRespectLimit() {
        saveThreat("a", vector(1f), Severity.HIGH, Set.of());
        saveThreat("b", vector(0.95f, 0.05f), Severity.HIGH, Set.of());
        saveThreat("c", vector(0.9f, 0.1f), Severity.HIGH, Set.of());

        ResponseEntity<SemanticSearchResponse> response = search("q=spring&limit=1", token);

        assertThat(response.getBody().threats()).hasSize(1);
    }

    @Test
    void search_shouldFallBackToKeywordSearch_whenEmbeddingIsUnavailable() {
        when(embeddingService.embedQuery(anyString())).thenReturn(null);
        saveThreat("match", null, Severity.HIGH, Set.of());
        saveThreat("other", null, Severity.HIGH, Set.of());

        // The title of a saved threat contains its unique external id
        ResponseEntity<SemanticSearchResponse> response =
                search("q=" + prefix + "match&limit=50", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownNames(response)).containsExactly("match");
    }

    @Test
    void search_shouldReturn401WithChallengeHeader_whenTokenIsMissing() {
        ResponseEntity<SemanticSearchResponse> response = search("q=spring", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }
}
