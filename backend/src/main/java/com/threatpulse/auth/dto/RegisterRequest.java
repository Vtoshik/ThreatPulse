package com.threatpulse.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) used to encapsulate user registration data.
 * An account needs only a nickname and a password. Validation annotations keep the
 * nickname short and simple and require a minimum password length.
 */
@Setter
@Getter
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain only letters, digits, '_', '.' and '-'")
    private String username;
    @Size(min = 8, max = 128)
    @NotBlank
    private String password;
}
