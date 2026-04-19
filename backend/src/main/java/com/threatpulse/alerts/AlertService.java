package com.threatpulse.alerts;

import com.threatpulse.alerts.dto.AlertHistoryResponse;
import com.threatpulse.alerts.dto.AlertRuleRequest;
import com.threatpulse.alerts.dto.AlertRuleResponse;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRuleRepository alertRuleRepository;
    private final EmailNotifier emailNotifier;
    private final AlertHistoryRepository alertHistoryRepository;
    private final ThreatRepository threatRepository;

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

        List<AlertRule> foundMatchingRules = alertRuleRepository.findMatchingRules(
                severity.name(), event.affectedTechnologies().toArray(new String[0]));

        // for each rule - get user, send email
        User[] users = foundMatchingRules.stream()
                .map(AlertRule::getUser)
                .toArray(User[]::new);

        String[] emailsToNotify = Arrays.stream(users)
                .map(User::getEmail)
                .toArray(String[]::new);

        if (emailsToNotify.length == 0) {
            return;
        }

        emailNotifier.sendAlert(emailsToNotify, event.title(), event.aiSummary());
        Threat threat = threatRepository.findByExternalId(event.externalId())
                .orElseThrow(() -> new IllegalStateException(
                        "Threat not found for externalId=" + event.externalId()
                ));

        for (User user : users) {
            AlertHistory alertHistory = new AlertHistory();
            alertHistory.setThreat(threat);
            alertHistory.setChannel("EMAIL");
            alertHistory.setUser(user);
            alertHistoryRepository.save(alertHistory);
        }
    }

    private AlertRuleResponse mapAlertRuleToResponseObject(AlertRule alertRule) {
        return new AlertRuleResponse(
                alertRule.getId(),
                alertRule.getMinSeverity(),
                alertRule.getTechnologiesFilter(),
                alertRule.isActive()
        );
    }

    public List<AlertRuleResponse> getRulesForUser(User user) {
        List<AlertRule> rules = alertRuleRepository.findByUser(user);
        return rules.stream()
                .map(this::mapAlertRuleToResponseObject).toList();
    }

    public AlertRuleResponse createRule(User user, AlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setUser(user);
        rule.setActive(true);
        rule.setMinSeverity(request.minSeverity());
        rule.setTechnologiesFilter(request.technologiesFilter());
        alertRuleRepository.save(rule);
        return mapAlertRuleToResponseObject(rule);
    }

    public AlertRuleResponse updateRule(Long id, User user, AlertRuleRequest request) throws AccessDeniedException {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "AlertRule not found for Id=" + id
                ));

        if (!alertRule.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Found rule does not belong to this User");
        }

        alertRule.setTechnologiesFilter(request.technologiesFilter());
        alertRule.setMinSeverity(request.minSeverity());
        alertRuleRepository.save(alertRule);
        return mapAlertRuleToResponseObject(alertRule);
    }

    public boolean deleteRule(Long id, User user) throws AccessDeniedException {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                "AlertRule not found for Id=" + id));
        if (!alertRule.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Found rule does not belong to this User");
        }
        alertRuleRepository.delete(alertRule);
        return true;
    }

    public AlertHistoryResponse[] getHistoryForUser(User user) {
        return alertHistoryRepository.findByUser(user)
                .stream().map(history -> new AlertHistoryResponse(
                        history.getId(),
                        history.getThreat().getId(),
                        history.getThreat().getTitle(),
                        history.getChannel(),
                        history.getSentAt()
                )).toArray(AlertHistoryResponse[]::new);
    }
}
