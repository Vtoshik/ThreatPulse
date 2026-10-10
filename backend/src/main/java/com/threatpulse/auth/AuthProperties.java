package com.threatpulse.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Switches that decide which account features exist in this deployment.
 * <p>
 * The hosted demo turns registration and credential changes off (the demo account is
 * shared), the local installation turns the demo account off.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    /** Whether POST /api/auth/register creates accounts. */
    private boolean registrationEnabled = false;
    /** Whether POST /api/auth/demo returns a token for the shared demo account. */
    private boolean demoEnabled = false;
    /** Whether a user may change the nickname and password. */
    private boolean credentialsChangeEnabled = false;
}
