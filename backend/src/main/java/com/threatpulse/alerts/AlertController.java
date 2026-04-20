package com.threatpulse.alerts;

import com.threatpulse.alerts.dto.AlertHistoryResponse;
import com.threatpulse.alerts.dto.AlertRuleRequest;
import com.threatpulse.alerts.dto.AlertRuleResponse;
import com.threatpulse.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping("/rules")
    public List<AlertRuleResponse> getAllAlertRules(@AuthenticationPrincipal User user) {
        return alertService.getRulesForUser(user);
    }

    @PostMapping("/rules")
    public AlertRuleResponse createNewAlertRule(
            @AuthenticationPrincipal User user,
            @RequestBody AlertRuleRequest request) {
        return alertService.createRule(user, request);
    }

    @PutMapping("rules/{id}")
    public AlertRuleResponse updateAlertRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody AlertRuleRequest request) throws AccessDeniedException {
        return alertService.updateRule(id, user, request);
    }

    @DeleteMapping("rules/{id}")
    public boolean deleteAlertRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) throws AccessDeniedException {
        return alertService.deleteRule(id, user);
    }

    @GetMapping("history")
    public AlertHistoryResponse[] getAlertsHistory(@AuthenticationPrincipal User user) {
        return alertService.getHistoryForUser(user);
    }
}
