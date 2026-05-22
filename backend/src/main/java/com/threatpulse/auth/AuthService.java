package com.threatpulse.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;

/**
 * Service responsible for authentication and authorization processes.
 * Handles user registration and login by validating input data, managing
 * user persistence, and generating JWT tokens for authenticated sessions.
 * Integrates with:
 * - UserRepository for database operations
 * - PasswordEncoder for secure password hashing and verification
 * - JwtService for token creation
 * - UserDetailsService for loading user-specific security data
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ThreatPulseException("Email already in use", 409);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ThreatPulseException("Username already in use", 409);
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);

        // loadUserByUsername uses email as identifier in this project
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return new AuthResponse(jwtService.generateToken(userDetails), "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        User foundUser = userRepository
                .findByEmail(request.getEmail()).orElseThrow(()
                        -> new ThreatPulseException("User with this email does not exist", 401));

        if (!passwordEncoder.matches(request.getPassword(), foundUser.getPasswordHash())) {
            throw new ThreatPulseException("Invalid credentials", 401);
        }

        // loadUserByUsername uses email as identifier in this project
        UserDetails userDetails = userDetailsService.loadUserByUsername(foundUser.getEmail());
        return new AuthResponse(jwtService.generateToken(userDetails), "Bearer");
    }
}
