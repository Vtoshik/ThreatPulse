package com.threatpulse.alerts.dto;

import com.threatpulse.common.domain.Severity;

public record AlertRuleResponse (
    Long id,
    String name,
    Severity minSeverity,
    String[] technologiesFilter,
    boolean active
) { }
