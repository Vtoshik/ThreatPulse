package com.threatpulse.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
    private JwtService jwtService;

    // test secret - must be valid Base64, minimum 32 bytes
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2gxMjM=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // @Value fields are not injected in unit tests
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiryHours", 24L);
    }

    // helper - creates a fake UserDetails with given email
    private UserDetails userWith(String email) {
        return User.builder().username(email).password("irrelevant")
                .authorities(Collections.emptyList()).build();
    }

    @Test
    void generateToken_shouldReturnNonEmptyToken() {
        UserDetails user = userWith("test@gmail.com");
        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(token).isNotNull();
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        UserDetails user = userWith("bob@gmail.com");
        String token = jwtService.generateToken(user);
        String email = jwtService.extractUsername(token);

        assertThat(email).isEqualTo(user.getUsername());
        assertThat(email).isNotBlank();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        UserDetails user1 = userWith("john@gmail.com");
        UserDetails user2 = userWith("alex@gmail.com");
        String token = jwtService.generateToken(user1);
        Boolean result = jwtService.isTokenValid(token, user2);

        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        UserDetails user = userWith("valid@gmail.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
