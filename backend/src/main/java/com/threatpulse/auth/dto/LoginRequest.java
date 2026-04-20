package com.threatpulse.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for user login requests.
 * Encapsulates the credentials required for authentication, including email and password.
 * Validation annotations ensure that the email and password field is not blank
 * and email follows a valid format.
 */
@Setter
@Getter
public class LoginRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
}
