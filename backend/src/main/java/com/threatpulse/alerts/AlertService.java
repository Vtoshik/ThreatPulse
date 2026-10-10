package com.threatpulse.alerts;

import com.threatpulse.alerts.dto.AlertHistoryResponse;
import com.threatpulse.alerts.dto.AlertRuleRequest;
import com.threatpulse.alerts.dto.AlertRuleResponse;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.TechnologyNames;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.user.User;
import com.threatpulse.user.UserPreferences;
import com.threatpulse.user.UserPreferencesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final String IN_APP_CHANNEL = "IN_APP";

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final ThreatRepository threatRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Optional outgoing channels. The list stays empty when no channel is enabled.
    private List<Notifier> notifiers = List.of();

    @Nullable
    @Autowired(required = false)
    private KafkaTemplate<String, AlertTriggerEvent> kafkaTemplate;

    @Autowired(required = false)
    public void setNotifiers(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    @Transactional
    public void checkAndNotify(AnalyzedThreatEvent event) {
        // find matching rules
        Severity severity;
        try {
            severity = Severity.valueOf(event.severity());
        } catch (Exception e) {
            log.error("Failed to cast severity type: {}", event.severity(), e);
            return;
        }

        // Guard: convert affectedTechnologies to array, handle null case
        String[] techArray = TechnologyNames
            .normalizeAll(event.affectedTechnologies()).toArray(new String[0]);

        List<AlertRule> foundMatchingRules =
            alertRuleRepository.findMatchingRules(severity.name(), techArray);

        if (foundMatchingRules.isEmpty()) {
            return;
        }

        // Ensure threat exists before sending alert
        Threat threat = threatRepository
            .findByExternalId(event.externalId())
            .orElseThrow(() ->
                new IllegalStateException(
                    "Threat not found for externalId=" + event.externalId()
                )
            );

        for (AlertRule rule : foundMatchingRules) {
            sendAlertToUser(rule.getUser(), threat, event.title(), event.aiSummary(), severity);
        }
    }

    @Transactional
    public void checkAndNotifyForThreat(Threat threat) {
        if (threat.getSeverity() == null || threat.getAiSummary() == null) return;

        String[] techArray = TechnologyNames
            .normalizeAll(threat.getAffectedTechnologies()).toArray(new String[0]);

        List<AlertRule> matchingRules = alertRuleRepository
            .findMatchingRules(threat.getSeverity().name(), techArray);

        for (AlertRule rule : matchingRules) {
            sendAlertToUser(rule.getUser(), threat, threat.getTitle(),
                threat.getAiSummary(), threat.getSeverity());
        }
    }

    /**
     * Creates the in-app alert and, if the user wants it, sends it through the optional
     * channels as well.
     * <p>
     * The history row is the record that "this user was told about this threat", and a
     * row stops every later attempt. So it is written only when nothing is left to
     * retry: a temporary delivery failure leaves no row, and the scheduler tries again
     * on its next run. A permanent failure is logged and the in-app alert still counts,
     * otherwise a wrong API key would be retried on every run.
     */
    private void sendAlertToUser(User user, Threat threat, String title,
                                 String summary, Severity severity) {
        if (alertHistoryRepository.existsByUserIdAndThreatId(user.getId(), threat.getId())) {
            return;
        }

        String channel = IN_APP_CHANNEL;
        if (!notifiers.isEmpty() && wantsOutgoingAlerts(user)) {
            AlertMessage message = new AlertMessage(title, summary, severity, threat.getSourceUrl());
            for (Notifier notifier : notifiers) {
                NotificationResult result = notifier.send(message);
                if (result == NotificationResult.RETRYABLE_FAILURE) {
                    log.warn("Alert for threat {} not recorded, {} delivery will be retried",
                            threat.getId(), notifier.channel());
                    return;
                }
                if (result == NotificationResult.SENT) {
                    channel = notifier.channel();
                }
            }
        }

        AlertHistory history = new AlertHistory();
        history.setThreat(threat);
        history.setChannel(channel);
        history.setUser(user);
        alertHistoryRepository.save(history);

        AlertTriggerEvent trigger =
            new AlertTriggerEvent(user.getId(), title, severity, threat.getId());
        if (kafkaTemplate != null) {
            kafkaTemplate.send(KafkaConfig.ALERT_TRIGGERS_TOPIC, trigger);
        } else {
            // No message bus: deliver the live notification inside this process
            eventPublisher.publishEvent(trigger);
        }
    }

    private boolean wantsOutgoingAlerts(User user) {
        return userPreferencesRepository.findByUser(user)
            .map(UserPreferences::isEmailAlertsEnabled)
            .orElse(false);
    }

    private AlertRuleResponse mapAlertRuleToResponseObject(
        AlertRule alertRule
    ) {
        return new AlertRuleResponse(
            alertRule.getId(),
            alertRule.getName(),
            alertRule.getMinSeverity(),
            alertRule.getTechnologiesFilter(),
            alertRule.isActive()
        );
    }

    public List<AlertRuleResponse> getRulesForUser(User user) {
        List<AlertRule> rules = alertRuleRepository.findByUser(user);
        return rules.stream().map(this::mapAlertRuleToResponseObject).toList();
    }

    public AlertRuleResponse createRule(User user, AlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setUser(user);
        rule.setName(request.name());
        rule.setActive(true);
        rule.setMinSeverity(request.minSeverity());
        rule.setTechnologiesFilter(TechnologyNames.normalizeAll(request.technologiesFilter()));
        alertRuleRepository.save(rule);
        return mapAlertRuleToResponseObject(rule);
    }

    public AlertRuleResponse updateRule(
        Long id,
        User user,
        AlertRuleRequest request
    ) throws AccessDeniedException {
        AlertRule alertRule = alertRuleRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalStateException("AlertRule not found for Id=" + id)
            );

        if (!alertRule.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                "Found rule does not belong to this User"
            );
        }

        if (request.name() != null) {
            alertRule.setName(request.name());
        }
        alertRule.setTechnologiesFilter(TechnologyNames.normalizeAll(request.technologiesFilter()));
        alertRule.setMinSeverity(request.minSeverity());
        if (request.active() != null) {
            alertRule.setActive(request.active());
        }
        alertRuleRepository.save(alertRule);
        return mapAlertRuleToResponseObject(alertRule);
    }

    public boolean deleteRule(Long id, User user) throws AccessDeniedException {
        AlertRule alertRule = alertRuleRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalStateException("AlertRule not found for Id=" + id)
            );
        if (!alertRule.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                "Found rule does not belong to this User"
            );
        }
        alertRuleRepository.delete(alertRule);
        return true;
    }

    public AlertHistoryResponse[] getHistoryForUser(User user) {
        return alertHistoryRepository
            .findByUser(user)
            .stream()
            .map(history ->
                new AlertHistoryResponse(
                    history.getId(),
                    history.getThreat().getId(),
                    history.getThreat().getTitle(),
                    history.getChannel(),
                    history.getSentAt()
                )
            )
            .toArray(AlertHistoryResponse[]::new);
    }
}
