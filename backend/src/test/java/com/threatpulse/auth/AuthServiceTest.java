package com.threatpulse.auth;

import com.threatpulse.auth.dto.AuthConfigResponse;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.common.exception.ThreatPulseException;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthProperties authProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setRegistrationEnabled(true);
        authProperties.setDemoEnabled(true);
        authProperties.setCredentialsChangeEnabled(true);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authProperties, true);
    }

    private static RegisterRequest registerRequest(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void register_shouldReturnToken_whenUsernameIsNew() {
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(registerRequest("testuser", "password123"));

        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("testuser", "password123")))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Username already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldBeRejected_whenRegistrationIsDisabled() {
        authProperties.setRegistrationEnabled(false);

        assertThatThrownBy(() -> authService.register(registerRequest("testuser", "password123")))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Registration is disabled on this server")
                .satisfies(e -> assertThat(((ThreatPulseException) e).getStatusCode()).isEqualTo(403));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        User fakeUser = new User("testuser", "hashedPassword");
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(fakeUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("testuser", "wrongpassword")))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_shouldGiveTheSameAnswer_whenTheUsernameDoesNotExist() {
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        // The message must not reveal whether the nickname exists
        assertThatThrownBy(() -> authService.login(loginRequest("nobody", "password123")))
                .isInstanceOf(ThreatPulseException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreCorrect() {
        User fakeUser = new User("testuser", "hashedPassword");
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(fakeUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(loginRequest("testuser", "password123"));

        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
    }

    @Test
    void loginDemo_shouldBeRejected_whenDemoIsDisabled() {
        authProperties.setDemoEnabled(false);

        assertThatThrownBy(() -> authService.loginDemo())
                .isInstanceOf(ThreatPulseException.class)
                .satisfies(e -> assertThat(((ThreatPulseException) e).getStatusCode()).isEqualTo(404));
    }

    @Test
    void loginDemo_shouldReturnToken_whenDemoUserExists() {
        User demo = new User("demo", "hash");
        when(userRepository.findByUsernameIgnoreCase("demo")).thenReturn(Optional.of(demo));
        when(jwtService.generateToken(demo)).thenReturn("demo.token");

        assertThat(authService.loginDemo().getAccessToken()).isEqualTo("demo.token");
    }

    @Test
    void getConfig_shouldReportTheSwitches() {
        authProperties.setRegistrationEnabled(false);
        authProperties.setDemoEnabled(true);
        authProperties.setCredentialsChangeEnabled(false);

        AuthConfigResponse config = authService.getConfig();

        assertThat(config.registrationEnabled()).isFalse();
        assertThat(config.demoEnabled()).isTrue();
        assertThat(config.credentialsChangeEnabled()).isFalse();
        assertThat(config.emailAlertsAvailable()).isTrue();
    }
}
