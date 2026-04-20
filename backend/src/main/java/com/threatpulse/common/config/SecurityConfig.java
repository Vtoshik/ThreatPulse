package com.threatpulse.common.config;

import com.threatpulse.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class.
 * Configures authentication, authorization, password encoding, and security filters.
 * Key configurations include:
 * - Disabling CSRF (since JWT is stateless)
 * - Setting session management to STATELESS
 * - Permitting public access to authentication endpoints and health checks
 * - Requiring authentication for all other endpoints
 * - Adding JwtAuthFilter before the default UsernamePasswordAuthenticationFilter
 * - Defining Argon2PasswordEncoder as the password encoder
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because we are using stateless JWT authentication
            .csrf(csrf -> csrf.disable())
                // Configure stateless session management (no HTTP session)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
                // Configure endpoint authorization
            .authorizeHttpRequests(auth -> auth
                    // Allow all requests to authentication endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    // Allow health check endpoint
                    .requestMatchers("/actuator/health").permitAll()
                    // Require authentication for all other endpoints
                    .anyRequest().authenticated()
                    // Add JWT filter before the default username/password authentication filter
            ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Defines the password encoder bean using Argon2 algorithm.
     * Parameters:
     * - salt length: 16 bytes
     * - hash length: 32 bytes
     * - parallelism: 1
     * - memory: 65536 KB
     * - iterations: 3
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}
