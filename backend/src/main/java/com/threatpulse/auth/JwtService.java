package com.threatpulse.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiry-hours}")
    private long expiryHours;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    String generateToken(UserDetails user) {
        return Jwts.builder().subject(user.getUsername()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryHours * 60 * 60 *1000))
                .signWith(getSigningKey()).compact();
    }

    String extractUsername(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    Date extractTokenExpiration(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getExpiration();
    }

    boolean isTokenValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && extractTokenExpiration(token).after(new Date());
    }
}
