package com.threatpulse.alerts;

import com.threatpulse.alerts.dto.AlertRuleRequest;
import com.threatpulse.alerts.dto.AlertRuleResponse;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {
    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private EmailNotifier emailNotifier;
    @Mock private AlertHistoryRepository alertHistoryRepository;
    @Mock private ThreatRepository threatRepository;

    @InjectMocks private AlertService alertService;

    @Test
    void deleteRule_shouldThrowException_whenRuleDoesNotBelongToUser(){
        User user1 = new User("Bob", "bob@example.com", "password");
        User user2 = new User("Alex", "alex@example.com", "hardpassword");

        user1.setId(1L);
        user2.setId(2L);

        AlertRule alertRule = new AlertRule();
        alertRule.setId(1L);
        alertRule.setUser(user1);

        when(alertRuleRepository.findById(1L)).thenReturn(java.util.Optional.of(alertRule));
        assertThatThrownBy(() -> alertService.deleteRule(1L, user2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Found rule does not belong to this User");
    }

    @Test
    void deleteRule_shouldDeleteRule_whenRulesBelongsToUser(){
        User user1 = new User("Maria", "maria@example.com", "password123");
        user1.setId(1L);

        AlertRule alertRule = new AlertRule();
        alertRule.setId(2L);
        alertRule.setUser(user1);

        when(alertRuleRepository.findById(2L)).thenReturn(java.util.Optional.of(alertRule));
        alertService.deleteRule(2L, user1);

        verify(alertRuleRepository).delete(alertRule);
    }

    @Test
    void createRule_shouldSaveAndReturnResponse() {
        User user = new User("John", "john@example.com", "pass");
        user.setId(1L);

        AlertRuleRequest request = new AlertRuleRequest(
                Severity.HIGH,
                new String[]{"kafka"}
        );

        AlertRuleResponse response = alertService.createRule(user, request);

        verify(alertRuleRepository).save(any(AlertRule.class));

        assertThat(response).isNotNull();
        assertThat(response.minSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void checkAndNotify_shouldNotSendEmail_whenNoMatchingRules() {
        // given
        when(alertRuleRepository.findMatchingRules(any(), any()))
                .thenReturn(java.util.List.of());

        AnalyzedThreatEvent event = new AnalyzedThreatEvent(
                "ext-1", "Test title", "description", "summary",
                "HIGH", "OTHER", java.util.List.of("kafka"),
                "action", "http://example.com", "test-source",
                java.time.OffsetDateTime.now()
        );

        // when
        alertService.checkAndNotify(event);

        // then
        verify(emailNotifier, org.mockito.Mockito.never())
                .sendAlert(any(), any(), any());
    }

    @Test
    void getRulesForUser_shouldReturnMappedResponses() {
        User user = new User("Anna", "anna@example.com", "pass");
        user.setId(1L);

        AlertRule rule1 = new AlertRule();
        rule1.setId(1L);
        rule1.setUser(user);
        rule1.setMinSeverity(Severity.HIGH);
        rule1.setTechnologiesFilter(new String[]{"kafka"});

        AlertRule rule2 = new AlertRule();
        rule2.setId(2L);
        rule2.setUser(user);
        rule2.setMinSeverity(Severity.LOW);
        rule2.setTechnologiesFilter(new String[]{"java"});

        when(alertRuleRepository.findByUser(user))
                .thenReturn(java.util.List.of(rule1, rule2));

        var result = alertService.getRulesForUser(user);

        assertThat(result).hasSize(2);
    }
}
