
package com.threatpulse.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate a JWT token with subject, issued date, expiration, and signature
    String generateToken(UserDetails user) {
        return Jwts.builder().subject(user.getUsername()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryHours * 60 * 60 *1000))
                .signWith(getSigningKey()).compact();
    }

    // Extract username (subject) from the JWT token
    String extractUsername(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    // Extract the expiration date from the JWT token
    Date extractTokenExpiration(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getExpiration();
    }

    // Validate the token by checking username match and expiration
    boolean isTokenValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && extractTokenExpiration(token).after(new Date());
    }
}