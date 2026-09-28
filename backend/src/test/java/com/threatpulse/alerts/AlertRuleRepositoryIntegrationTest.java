package com.threatpulse.alerts;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs findMatchingRules against a real PostgreSQL container.
 * <p>
 * The query compares a native enum column (severity_level_enum) with a parameter,
 * so mocked repository tests cannot catch type or ordering mistakes in it.
 * Enum order is CRITICAL < HIGH < MEDIUM < LOW < INFO, so a rule with min severity HIGH
 * must match CRITICAL and HIGH threats, but not MEDIUM, LOW or INFO.
 */
public class AlertRuleRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        // Remove rules seeded by migrations (demo user) so each test controls its own data
        alertRuleRepository.deleteAll();

        String unique = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(new User("user-" + unique, unique + "@example.com", "hash"));
    }

    private AlertRule saveRule(Severity minSeverity, String[] technologiesFilter, boolean active) {
        AlertRule rule = new AlertRule();
        rule.setUser(user);
        rule.setName("rule");
        rule.setMinSeverity(minSeverity);
        rule.setTechnologiesFilter(technologiesFilter);
        rule.setActive(active);
        return alertRuleRepository.save(rule);
    }

    private static final String[] NO_TECHNOLOGIES = new String[0];

    @Test
    void findMatchingRules_shouldMatch_whenThreatIsMoreSevereThanRuleMinimum() {
        AlertRule rule = saveRule(Severity.HIGH, null, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules("CRITICAL", NO_TECHNOLOGIES);

        assertThat(result).extracting(AlertRule::getId).containsExactly(rule.getId());
    }

    @Test
    void findMatchingRules_shouldMatch_whenThreatHasSameSeverityAsRuleMinimum() {
        AlertRule rule = saveRule(Severity.HIGH, null, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules("HIGH", NO_TECHNOLOGIES);

        assertThat(result).extracting(AlertRule::getId).containsExactly(rule.getId());
    }

    @Test
    void findMatchingRules_shouldNotMatch_whenThreatIsLessSevereThanRuleMinimum() {
        saveRule(Severity.HIGH, null, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules("MEDIUM", NO_TECHNOLOGIES);

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingRules_shouldIgnoreInactiveRules() {
        saveRule(Severity.LOW, null, false);

        List<AlertRule> result = alertRuleRepository.findMatchingRules("CRITICAL", NO_TECHNOLOGIES);

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingRules_shouldMatch_whenTechnologiesOverlap() {
        AlertRule rule = saveRule(Severity.LOW, new String[]{"spring-boot", "redis"}, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules(
                "HIGH", new String[]{"redis", "kafka"});

        assertThat(result).extracting(AlertRule::getId).containsExactly(rule.getId());
    }

    @Test
    void findMatchingRules_shouldNotMatch_whenTechnologiesDoNotOverlap() {
        saveRule(Severity.LOW, new String[]{"spring-boot"}, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules(
                "HIGH", new String[]{"postgresql"});

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingRules_shouldMatchAnyTechnology_whenRuleHasNoTechnologyFilter() {
        AlertRule rule = saveRule(Severity.LOW, null, true);

        List<AlertRule> result = alertRuleRepository.findMatchingRules(
                "HIGH", new String[]{"anything"});

        assertThat(result).extracting(AlertRule::getId).containsExactly(rule.getId());
    }
}
