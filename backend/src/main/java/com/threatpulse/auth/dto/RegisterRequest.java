package com.threatpulse.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Data Transfer Object (DTO) used to encapsulate user registration data.
 * This class carries the required information for creating a new user account,
 * Validation annotations ensure that the provided data meets basic constraints
 * such as non-empty email, valid email format, and minimum password length.
 */
@Getter
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    @Email
    private String email;
    @Size(min=8)
    @NotBlank
    private String password;
}
