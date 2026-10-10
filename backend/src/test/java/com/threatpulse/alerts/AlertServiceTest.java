package com.threatpulse.alerts;

import com.threatpulse.alerts.dto.AlertRuleRequest;
import com.threatpulse.alerts.dto.AlertRuleResponse;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.user.User;
import com.threatpulse.user.UserPreferences;
import com.threatpulse.user.UserPreferencesRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {
    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertHistoryRepository alertHistoryRepository;
    @Mock private ThreatRepository threatRepository;
    @Mock private UserPreferencesRepository userPreferencesRepository;
    @Mock private Notifier notifier;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private AlertService alertService;

    @BeforeEach
    void addNotifier() {
        alertService.setNotifiers(java.util.List.of(notifier));
    }

    private static Threat threat(long id) {
        Threat threat = new Threat();
        threat.setId(id);
        threat.setTitle("Spring RCE");
        threat.setAiSummary("A remote code execution flaw");
        threat.setSeverity(Severity.CRITICAL);
        threat.setSourceUrl("https://example.com/advisory");
        threat.setAffectedTechnologies(java.util.Set.of("spring-boot"));
        return threat;
    }

    private AlertRule matchingRuleFor(User user) {
        AlertRule rule = new AlertRule();
        rule.setUser(user);
        when(alertRuleRepository.findMatchingRules(any(), any())).thenReturn(java.util.List.of(rule));
        return rule;
    }

    private void userWantsOutgoingAlerts(User user, boolean enabled) {
        UserPreferences preferences = new UserPreferences();
        preferences.setEmailAlertsEnabled(enabled);
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
    }

    @Test
    void deleteRule_shouldThrowException_whenRuleDoesNotBelongToUser(){
        User user1 = new User("Bob", "password");
        User user2 = new User("Alex", "hardpassword");

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
        User user1 = new User("Maria", "password123");
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
        User user = new User("John", "pass");
        user.setId(1L);

        AlertRuleRequest request = new AlertRuleRequest(
                "Test rule",
                Severity.HIGH,
                new String[]{"kafka"},
                null
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
        verify(notifier, never()).send(any());
        verify(alertHistoryRepository, never()).save(any());
    }

    @Test
    void checkAndNotifyForThreat_shouldRecordTheChannel_whenDeliverySucceeds() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, true);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);
        when(notifier.channel()).thenReturn("EMAIL");
        when(notifier.send(any())).thenReturn(NotificationResult.SENT);

        alertService.checkAndNotifyForThreat(threat(10L));

        ArgumentCaptor<AlertHistory> saved = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo("EMAIL");
    }

    @Test
    void checkAndNotifyForThreat_shouldNotRecordHistory_whenDeliveryMustBeRetried() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, true);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);
        when(notifier.channel()).thenReturn("EMAIL");
        when(notifier.send(any())).thenReturn(NotificationResult.RETRYABLE_FAILURE);

        alertService.checkAndNotifyForThreat(threat(10L));

        // A history row would stop every later attempt, and the alert would be lost
        verify(alertHistoryRepository, never()).save(any());
    }

    @Test
    void checkAndNotifyForThreat_shouldKeepTheInAppAlert_whenDeliveryFailsForGood() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, true);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);
        when(notifier.send(any())).thenReturn(NotificationResult.PERMANENT_FAILURE);

        alertService.checkAndNotifyForThreat(threat(10L));

        ArgumentCaptor<AlertHistory> saved = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo("IN_APP");
    }

    @Test
    void checkAndNotifyForThreat_shouldNotCallNotifiers_whenUserDidNotAskForThem() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, false);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);

        alertService.checkAndNotifyForThreat(threat(10L));

        verify(notifier, never()).send(any());
        ArgumentCaptor<AlertHistory> saved = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo("IN_APP");
    }

    @Test
    void checkAndNotifyForThreat_shouldPublishLiveNotificationInProcess_whenThereIsNoKafka() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, false);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);

        alertService.checkAndNotifyForThreat(threat(10L));

        ArgumentCaptor<AlertTriggerEvent> published = ArgumentCaptor.forClass(AlertTriggerEvent.class);
        verify(eventPublisher).publishEvent(published.capture());
        assertThat(published.getValue().userId()).isEqualTo(1L);
        assertThat(published.getValue().threatId()).isEqualTo(10L);
    }

    @Test
    void checkAndNotifyForThreat_shouldNotPublishLiveNotification_whenDeliveryMustBeRetried() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        userWantsOutgoingAlerts(user, true);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(false);
        when(notifier.channel()).thenReturn("EMAIL");
        when(notifier.send(any())).thenReturn(NotificationResult.RETRYABLE_FAILURE);

        alertService.checkAndNotifyForThreat(threat(10L));

        // Otherwise the user would get a notification again on every retry
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void checkAndNotifyForThreat_shouldSkipUserWhoWasAlreadyNotified() {
        User user = new User("anna", "hash");
        user.setId(1L);
        matchingRuleFor(user);
        when(alertHistoryRepository.existsByUserIdAndThreatId(1L, 10L)).thenReturn(true);

        alertService.checkAndNotifyForThreat(threat(10L));

        verify(notifier, never()).send(any());
        verify(alertHistoryRepository, never()).save(any());
    }

    @Test
    void getRulesForUser_shouldReturnMappedResponses() {
        User user = new User("Anna", "pass");
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
