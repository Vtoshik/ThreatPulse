package com.threatpulse.user.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String[] technologies
) {}
