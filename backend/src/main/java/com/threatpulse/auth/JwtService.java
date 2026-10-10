
package com.threatpulse.auth;

import com.threatpulse.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Service responsible for generating, parsing, and validating JWT tokens.
 * Provides methods to:
 * - Generate a JWT for a given UserDetails object
 * - Extract the username from a JWT
 * - Extract the token's expiration date
 * - Validate the token against a user's details
 * The service uses a secret key defined in application properties and HMAC SHA signing.
 */
@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiry-hours}")
    private long expiryHours;

    // Decode the Base64-encoded secret key and create a HMAC SHA signing key
    private static final String TOKEN_VERSION_CLAIM = "ver";

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate a JWT token with subject, issued date, expiration, and signature
    public String generateToken(User user) {
        return Jwts.builder().subject(String.valueOf(user.getId()))
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryHours * 60 * 60 *1000))
                .signWith(getSigningKey()).compact();
    }

    // Extract id (subject) from the JWT token
    public Long extractUserId(String token) {
        String id = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new JwtException("Failed to parse String id to Long", e);
        }
    }

    // Extract the expiration date from the JWT token
    Date extractTokenExpiration(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getExpiration();
    }

    // Tokens issued before versions existed have no claim, which means version 0
    int extractTokenVersion(String token) {
        Integer version = Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload()
                .get(TOKEN_VERSION_CLAIM, Integer.class);
        return version == null ? 0 : version;
    }

    // Validate the token: same user, same token version, not expired
    public boolean isTokenValid(String token, User user) {
        final Long id = extractUserId(token);
        return id.equals(user.getId())
                && extractTokenVersion(token) == user.getTokenVersion()
                && extractTokenExpiration(token).after(new Date());
    }
}