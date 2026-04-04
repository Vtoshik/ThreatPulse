package com.threatpulse.common.domain;

/**
 * Enumeration representing severity levels.
 * <p>
 * Defines the possible levels of importance or impact for events,
 * issues, or vulnerabilities within the system.
 * <p>
 * Levels (from highest to lowest):
 * - CRITICAL: Severe impact requiring immediate attention
 * - HIGH: Significant impact
 * - MEDIUM: Moderate impact
 * - LOW: Minor impact
 * - INFO: Informational, no direct impact
 */
public enum Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}
