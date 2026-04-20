package com.threatpulse.alerts.dto;

import com.threatpulse.common.domain.Severity;

public record AlertRuleResponse (
    Long id,
    Severity minSeverity,
    String[] technologiesFilter,
    boolean active
) { }
