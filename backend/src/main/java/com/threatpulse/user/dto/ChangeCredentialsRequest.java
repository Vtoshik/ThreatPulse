package com.threatpulse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to change the nickname, the password, or both.
 * The current password is always required, so a stolen token alone is not enough to
 * take over the account. A field that is null is left unchanged.
 */
public record ChangeCredentialsRequest(
        @NotBlank String currentPassword,
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain only letters, digits, '_', '.' and '-'")
        String newUsername,
        @Size(min = 8, max = 128) String newPassword
) {}
