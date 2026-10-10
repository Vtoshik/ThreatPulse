package com.threatpulse.common.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TechnologyNamesTest {

    @Test
    void normalize_shouldLowerCaseTrimAndJoinWordsWithHyphen() {
        assertThat(TechnologyNames.normalize("  Spring Boot ")).isEqualTo("spring-boot");
        assertThat(TechnologyNames.normalize("Apache_Kafka")).isEqualTo("apache-kafka");
        assertThat(TechnologyNames.normalize("Spring   Framework")).isEqualTo("spring-framework");
    }

    @Test
    void normalize_shouldMapWellKnownAliases() {
        assertThat(TechnologyNames.normalize("Node.js")).isEqualTo("nodejs");
        assertThat(TechnologyNames.normalize("Postgres")).isEqualTo("postgresql");
        assertThat(TechnologyNames.normalize("PostgreSQL")).isEqualTo("postgresql");
        assertThat(TechnologyNames.normalize("K8s")).isEqualTo("kubernetes");
    }

    @Test
    void normalize_shouldKeepAlreadyCanonicalNamesUnchanged() {
        assertThat(TechnologyNames.normalize("spring-boot")).isEqualTo("spring-boot");
        assertThat(TechnologyNames.normalize("redis")).isEqualTo("redis");
    }

    @Test
    void normalize_shouldReturnEmpty_forNullAndBlank() {
        assertThat(TechnologyNames.normalize(null)).isEmpty();
        assertThat(TechnologyNames.normalize("   ")).isEmpty();
        assertThat(TechnologyNames.normalize("--")).isEmpty();
    }

    @Test
    void normalizeAll_shouldDropEmptyNamesAndDuplicatesAndKeepOrder() {
        List<String> names = Arrays.asList("Spring Boot", "spring-boot", null, " ", "Redis");

        assertThat(TechnologyNames.normalizeAll(names)).containsExactly("spring-boot", "redis");
    }

    @Test
    void normalizeAll_shouldReturnEmptySet_forNullCollection() {
        assertThat(TechnologyNames.normalizeAll((java.util.Collection<String>) null)).isEmpty();
    }

    @Test
    void normalizeAll_shouldKeepNullArrayNull_becauseNullMeansAllTechnologies() {
        assertThat(TechnologyNames.normalizeAll((String[]) null)).isNull();
    }
}
