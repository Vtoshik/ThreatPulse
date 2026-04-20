package com.threatpulse.alerts.dto;

import com.threatpulse.common.domain.Severity;

public record AlertRuleRequest(
        Severity minSeverity,
        String[] technologiesFilter
) {
}
