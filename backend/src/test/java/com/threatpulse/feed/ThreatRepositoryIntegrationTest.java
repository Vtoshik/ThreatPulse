package com.threatpulse.feed;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the pgvector similarity query against a real PostgreSQL + pgvector container.
 * <p>
 * Vectors are hand-made so the expected distances are known:
 * with the query vector [1, 0, 0, ...] the nearest threat is the identical one,
 * then the mostly-similar one, then the orthogonal one.
 */
public class ThreatRepositoryIntegrationTest extends BaseIntegrationTest {
    private static final int DIMENSIONS = 384;

    @Autowired
    private ThreatRepository threatRepository;

    @BeforeEach
    void cleanTable() {
        // The database is shared between tests in this class, so start from an empty table
        threatRepository.deleteAll();
    }

    /** Builds a 384-dimension vector whose first values are the given ones and the rest are zeros. */
    private static float[] vector(float... leadingValues) {
        float[] vector = new float[DIMENSIONS];
        System.arraycopy(leadingValues, 0, vector, 0, leadingValues.length);
        return vector;
    }

    private Threat saveThreat(String externalId, float[] embedding) {
        return saveThreat(externalId, embedding, Severity.HIGH);
    }

    private Threat saveThreat(String externalId, float[] embedding, Severity severity) {
        Threat threat = new Threat();
        threat.setExternalId(externalId);
        threat.setTitle("Title " + externalId);
        threat.setDescription("Description " + externalId);
        threat.setSourceUrl("https://example.com/" + externalId);
        threat.setSourceName("TEST");
        threat.setSeverity(severity);
        threat.setThreatCategory(ThreatCategory.OTHER);
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setEmbedding(embedding);
        return threatRepository.save(threat);
    }

    private static List<String> externalIds(List<Threat> threats) {
        return threats.stream().map(Threat::getExternalId).toList();
    }

    @Test
    void findNearest_shouldOrderThreatsByCosineDistance() {
        saveThreat("orthogonal", vector(0f, 1f));      // distance 1.0 from the query
        saveThreat("identical", vector(1f));           // distance 0.0
        saveThreat("similar", vector(0.9f, 0.1f));     // distance ~0.006

        String query = Arrays.toString(vector(1f));

        List<Threat> result = threatRepository.findNearest(query, null, 10);

        assertThat(externalIds(result)).containsExactly("identical", "similar", "orthogonal");
    }

    @Test
    void findNearest_shouldSkipThreatsWithoutEmbedding() {
        saveThreat("embedded", vector(1f));
        saveThreat("no-embedding", null);

        List<Threat> result = threatRepository.findNearest(Arrays.toString(vector(1f)), null, 10);

        // Without the IS NOT NULL filter the null-embedding row would fill the remaining slots
        assertThat(externalIds(result)).containsExactly("embedded");
    }

    @Test
    void findNearest_shouldRespectLimit() {
        saveThreat("a", vector(1f));
        saveThreat("b", vector(0.9f, 0.1f));
        saveThreat("c", vector(0f, 1f));

        List<Threat> result = threatRepository.findNearest(Arrays.toString(vector(1f)), null, 2);

        assertThat(externalIds(result)).containsExactly("a", "b");
    }

    @Test
    void findNearest_shouldReturnEmptyList_whenNoThreatHasEmbedding() {
        saveThreat("no-embedding", null);

        List<Threat> result = threatRepository.findNearest(Arrays.toString(vector(1f)), null, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void findNearest_shouldFilterBySeverity_whenSeverityIsGiven() {
        saveThreat("high", vector(1f), Severity.HIGH);
        saveThreat("critical", vector(0.9f, 0.1f), Severity.CRITICAL);

        List<Threat> result = threatRepository.findNearest(
                Arrays.toString(vector(1f)), Severity.CRITICAL.name(), 10);

        assertThat(externalIds(result)).containsExactly("critical");
    }

    @Test
    void findNearest_shouldIgnoreSeverityFilter_whenSeverityIsNull() {
        saveThreat("high", vector(1f), Severity.HIGH);
        saveThreat("critical", vector(0.9f, 0.1f), Severity.CRITICAL);

        List<Threat> result = threatRepository.findNearest(
                Arrays.toString(vector(1f)), null, 10);

        assertThat(externalIds(result)).containsExactly("high", "critical");
    }
}
