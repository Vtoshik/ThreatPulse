package com.threatpulse.auth;

import com.threatpulse.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private User userWithId(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "hash");
        user.setId(id);
        return user;
    }

    /** Signs a token with the test key but a chosen subject and expiration, like an old or expired token. */
    private String tokenWith(String subject, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.builder().subject(subject).issuedAt(new Date())
                .expiration(expiration).signWith(key).compact();
    }

    @Test
    void generateToken_shouldReturnNonEmptyToken() {
        String token = jwtService.generateToken(userWithId(1L));

        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserId_shouldReturnTheIdOfTheUser() {
        String token = jwtService.generateToken(userWithId(42L));

        Long id = jwtService.extractUserId(token);

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void generateToken_shouldNotPutEmailOrUsernameIntoToken() {
        User user = userWithId(7L);
        String token = jwtService.generateToken(user);

        // A JWT payload is only Base64 encoded: anyone holding the token can read it
        String payload = new String(Decoders.BASE64URL.decode(token.split("\\.")[1]));
        assertThat(payload).doesNotContain(user.getEmail()).doesNotContain(user.getUsername());
    }

    @Test
    void isTokenValid_shouldReturnTrue_forTheSameUser() {
        User user = userWithId(1L);
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnTrue_forLargeIds() {
        // Long objects above 127 are different instances after parsing, so == would fail here
        User user = userWithId(100_000L);
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, userWithId(100_000L))).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        String token = jwtService.generateToken(userWithId(1L));

        assertThat(jwtService.isTokenValid(token, userWithId(2L))).isFalse();
    }

    @Test
    void extractUserId_shouldThrowJwtException_forTokenWithOldStyleSubject() {
        // Tokens issued before the id subject contain an email or username
        String oldToken = tokenWith("someone@example.com", new Date(System.currentTimeMillis() + 60_000));

        // Must be a JwtException, so the filter treats it as an ordinary invalid token (401), not a 500
        assertThatThrownBy(() -> jwtService.extractUserId(oldToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_shouldThrowJwtException_forExpiredToken() {
        String expired = tokenWith("1", new Date(System.currentTimeMillis() - 60_000));

        assertThatThrownBy(() -> jwtService.extractUserId(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_shouldThrowJwtException_forGarbageToken() {
        assertThatThrownBy(() -> jwtService.extractUserId("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
