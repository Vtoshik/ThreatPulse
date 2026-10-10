package com.threatpulse.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.threatpulse.auth.dto.AuthConfigResponse;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;

/**
 * Service responsible for authentication processes.
 * Handles user registration and login by validating input data, managing
 * user persistence, and generating JWT tokens for authenticated sessions.
 * Which features exist is controlled by {@link AuthProperties}.
 */
@Service
public class AuthService {
    static final String DEMO_USERNAME = "demo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final boolean emailAlertsAvailable;
    // Compared against when the nickname does not exist, so a wrong nickname and a wrong
    // password take about the same time and the answer does not reveal which one it was
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthProperties authProperties,
                       @Value("${app.notifications.email.enabled:false}") boolean emailAlertsAvailable) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
        this.emailAlertsAvailable = emailAlertsAvailable;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    public AuthConfigResponse getConfig() {
        return new AuthConfigResponse(
                authProperties.isRegistrationEnabled(),
                authProperties.isDemoEnabled(),
                authProperties.isCredentialsChangeEnabled(),
                emailAlertsAvailable);
    }

    public AuthResponse register(RegisterRequest request) {
        if (!authProperties.isRegistrationEnabled()) {
            throw new ThreatPulseException("Registration is disabled on this server", 403);
        }
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ThreatPulseException("Username already in use", 409);
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), "Bearer");
    }

    public AuthResponse loginDemo() {
        if (!authProperties.isDemoEnabled()) {
            throw new ThreatPulseException("Demo account is disabled on this server", 404);
        }
        User demoUser = userRepository.findByUsernameIgnoreCase(DEMO_USERNAME)
                .orElseThrow(() -> new ThreatPulseException("Demo user not available", 503));
        return new AuthResponse(jwtService.generateToken(demoUser), "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        User foundUser = userRepository.findByUsernameIgnoreCase(request.getUsername()).orElse(null);

        if (foundUser == null) {
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash);
            throw new ThreatPulseException("Invalid credentials", 401);
        }
        if (!passwordEncoder.matches(request.getPassword(), foundUser.getPasswordHash())) {
            throw new ThreatPulseException("Invalid credentials", 401);
        }

        return new AuthResponse(jwtService.generateToken(foundUser), "Bearer");
    }
}
