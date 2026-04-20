package com.threatpulse.user.dto;

import com.threatpulse.common.domain.Severity;

public record UpdatePreferencesRequest(
   Severity minSeverity,
   boolean emailAlertsEnabled
) {}
