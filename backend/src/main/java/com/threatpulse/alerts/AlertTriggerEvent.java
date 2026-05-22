package com.threatpulse.alerts;

import com.threatpulse.common.domain.Severity;

public record AlertTriggerEvent(
    Long userId,
    String title,
    Severity severity,
    Long threatId
) { }
