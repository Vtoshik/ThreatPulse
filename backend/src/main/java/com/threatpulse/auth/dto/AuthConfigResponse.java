package com.threatpulse.auth.dto;

/**
 * Tells the frontend which account features this deployment has,
 * so it can hide forms that would be rejected anyway.
 */
public record AuthConfigResponse(
        boolean registrationEnabled,
        boolean demoEnabled,
        boolean credentialsChangeEnabled,
        boolean emailAlertsAvailable
) {}
