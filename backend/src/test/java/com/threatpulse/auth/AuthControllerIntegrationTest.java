package com.threatpulse.auth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.threatpulse.BaseIntegrationTest;
import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;

public class AuthControllerIntegrationTest extends BaseIntegrationTest{

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_shouldReturn200_withToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
    }

    @Test
    void login_afterRegister_shouldReturn200() {
        // first register the user
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testuser2");
        register.setEmail("login@example.com");
        register.setPassword("password123");
        restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);

        // then try to login
        LoginRequest login  = new LoginRequest();
        login.setEmail("login@example.com");
        login.setPassword("password123");

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isNotBlank();
    }
}
