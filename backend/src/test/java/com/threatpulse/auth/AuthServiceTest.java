package com.threatpulse.auth;

import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.user.User;
import com.threatpulse.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldReturnToken_whenEmailIsNew() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(request);
        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class).hasMessage("Email already in use");
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        User fakeUser = new User(
                "testuser", "test@example.com", "hashedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(fakeUser));
        when(passwordEncoder
                .matches("wrongpassword", "hashedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreCorrect() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User fakeUser = new User("testuser", "test@example.com", "hashedPassword");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(fakeUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(request);
        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
    }

    @Test
    void register_shouldNotSaveUser_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any(User.class));
    }
}
