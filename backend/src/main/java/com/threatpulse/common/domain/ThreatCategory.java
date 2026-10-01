package com.threatpulse.common.domain;

/**
 * Enumeration representing categories of security threats.
 * <p>
 * Defines common types of vulnerabilities and attack vectors.
 */
public enum ThreatCategory {
    RCE,         // Remote Code Execution
    XSS,         // Cross-Site Scripting
    SQLI,        // SQL Injection
    DATA_BREACH, // Data exposure or leak
    SUPPLY_CHAIN, // Compromised or malicious dependency, package or build tool
    OTHER        // Uncategorized threats
}
