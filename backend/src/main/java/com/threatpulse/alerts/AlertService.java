package com.threatpulse.alerts;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRuleRepository alertRuleRepository;
    private final EmailNotifier emailNotifier;

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
        String[] emailsToNotify = foundMatchingRules.stream()
                .map(rule -> rule.getUser().getEmail())
                .distinct()
                .toArray(String[]::new);

        if (emailsToNotify.length == 0) {
            return;
        }

        emailNotifier.sendAlert(emailsToNotify, event.title(), event.aiSummary());

        //alertRuleRepository.save()
    }
}
