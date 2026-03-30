package com.threatpulse.auth.dto;

import lombok.Getter;

/**
 * Data Transfer Object (DTO) representing the authentication response.
 * Contains the access token issued after successful authentication and the token type
 * (default is "Bearer"). This object is typically returned to the client and used
 * for authorizing subsequent requests.
 */
@Getter
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";

    public AuthResponse(String s, String bearer) {
        this.accessToken = s;
        this.tokenType = bearer;
    }

    public AuthResponse(String s) {
        this.accessToken = s;
    }
}
