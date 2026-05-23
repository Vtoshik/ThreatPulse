package com.threatpulse.alerts.dto;

import com.threatpulse.common.domain.Severity;

public record AlertRuleRequest(
        String name,
        Severity minSeverity,
        String[] technologiesFilter,
        Boolean active
) {
}
