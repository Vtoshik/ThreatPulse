package com.threatpulse.auth;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import com.threatpulse.user.dto.UserProfileResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses tokens on a real protected endpoint (GET /api/user/profile).
 * <p>
 * Login and register tests alone cannot catch a broken token check, because they only
 * create a token and never send it back. These tests cover the whole round trip:
 * the token subject written by JwtService, and the user lookup done by JwtAuthFilter.
 */
public class JwtAuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private record Registered(String username, String email, String password, String token) {}

    private Registered register() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user-" + unique);
        request.setEmail(unique + "@example.com");
        request.setPassword("password123");
        String token = restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class)
                .getBody().getAccessToken();
        return new Registered(request.getUsername(), request.getEmail(), request.getPassword(), token);
    }

    private ResponseEntity<UserProfileResponse> getProfile(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/user/profile", HttpMethod.GET,
                new HttpEntity<>(headers), UserProfileResponse.class);
    }

    /** Signs a token with the real test key and the given subject, like a token from an older version. */
    private String tokenWithSubject(String subject) {
        return Jwts.builder().subject(subject).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))).compact();
    }

    @Test
    void protectedEndpoint_shouldAccept_tokenFromRegister() {
        Registered user = register();

        ResponseEntity<UserProfileResponse> response = getProfile(user.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(user.email());
        assertThat(response.getBody().username()).isEqualTo(user.username());
    }

    @Test
    void protectedEndpoint_shouldAccept_tokenFromLogin() {
        Registered user = register();
        LoginRequest login = new LoginRequest();
        login.setEmail(user.email());
        login.setPassword(user.password());
        String loginToken = restTemplate.postForEntity("/api/auth/login", login, AuthResponse.class)
                .getBody().getAccessToken();

        ResponseEntity<UserProfileResponse> response = getProfile(loginToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(user.email());
    }

    @Test
    void protectedEndpoint_shouldReturn401_notServerError_forTokenWithOldStyleSubject() {
        Registered user = register();

        // Tokens issued before the id subject carry an email (or a username) instead of a number
        ResponseEntity<String> byEmail = restTemplate.exchange("/api/user/profile", HttpMethod.GET,
                bearer(tokenWithSubject(user.email())), String.class);
        ResponseEntity<String> byUsername = restTemplate.exchange("/api/user/profile", HttpMethod.GET,
                bearer(tokenWithSubject(user.username())), String.class);

        assertThat(byEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(byUsername.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_shouldReturn401_forTokenOfNonExistentUser() {
        // A validly signed token whose user id does not exist (for example a deleted account)
        ResponseEntity<String> response = restTemplate.exchange("/api/user/profile", HttpMethod.GET,
                bearer(tokenWithSubject("999999999")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_shouldReturn401_forGarbageToken() {
        ResponseEntity<String> response = restTemplate.exchange("/api/user/profile", HttpMethod.GET,
                bearer("not-a-jwt"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
