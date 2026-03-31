package com.threatpulse.auth;

import com.threatpulse.auth.dto.AuthResponse;
import com.threatpulse.auth.dto.LoginRequest;
import com.threatpulse.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new
            PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("threatpulse_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // override DB connection to use Testcontainers PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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
