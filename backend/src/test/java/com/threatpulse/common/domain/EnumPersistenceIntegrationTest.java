package com.threatpulse.common.domain;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.feed.ThreatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that every Java enum value can be stored in and read back from its
 * PostgreSQL enum type.
 * <p>
 * The database types are created by Flyway migrations and the Java enums are separate code,
 * so they can drift apart: a value added to one but not the other only fails when a real
 * row is written. These tests fail the build as soon as that happens.
 */
public class EnumPersistenceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ThreatRepository threatRepository;

    private Threat newThreat(Severity severity, ThreatCategory category) {
        Threat threat = new Threat();
        threat.setExternalId("enum-" + UUID.randomUUID());
        threat.setTitle("Title");
        threat.setDescription("Description");
        threat.setSourceUrl("https://example.com");
        threat.setSourceName("TEST");
        threat.setSeverity(severity);
        threat.setThreatCategory(category);
        threat.setPublishedAt(OffsetDateTime.now());
        threat.setCollectedAt(OffsetDateTime.now());
        return threat;
    }

    @Test
    void everyThreatCategory_shouldBeStoredAndReadBack() {
        for (ThreatCategory category : ThreatCategory.values()) {
            Threat saved = threatRepository.saveAndFlush(newThreat(Severity.HIGH, category));

            Threat loaded = threatRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getThreatCategory()).as("category %s", category).isEqualTo(category);
        }
    }

    @Test
    void everySeverity_shouldBeStoredAndReadBack() {
        for (Severity severity : Severity.values()) {
            Threat saved = threatRepository.saveAndFlush(newThreat(severity, ThreatCategory.OTHER));

            Threat loaded = threatRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getSeverity()).as("severity %s", severity).isEqualTo(severity);
        }
    }

    @Test
    void everyAnalysisStatus_shouldBeStoredAndReadBack() {
        for (AnalysisStatus status : AnalysisStatus.values()) {
            Threat threat = newThreat(Severity.HIGH, ThreatCategory.OTHER);
            threat.setAnalysisStatus(status);
            Threat saved = threatRepository.saveAndFlush(threat);

            Threat loaded = threatRepository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getAnalysisStatus()).as("status %s", status).isEqualTo(status);
        }
    }

    @Test
    void newThreat_shouldDefaultToAnalyzed() {
        Threat saved = threatRepository.saveAndFlush(newThreat(Severity.HIGH, ThreatCategory.OTHER));

        assertThat(threatRepository.findById(saved.getId()).orElseThrow().getAnalysisStatus())
                .isEqualTo(AnalysisStatus.ANALYZED);
    }

    @Test
    void pendingThreat_shouldBeStoredWithoutSeverityCategoryAndAnalysisTime() {
        // A threat waiting for analysis has unknown values, which must be NULL and not made up
        Threat threat = newThreat(null, null);
        threat.setAnalysisStatus(AnalysisStatus.PENDING_ANALYSIS);
        threat.setAnalysisAttemptedAt(OffsetDateTime.now());
        Threat saved = threatRepository.saveAndFlush(threat);

        Threat loaded = threatRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getSeverity()).isNull();
        assertThat(loaded.getThreatCategory()).isNull();
        assertThat(loaded.getAnalyzedAt()).isNull();
        assertThat(loaded.getAnalysisAttemptedAt()).isNotNull();
    }
}
