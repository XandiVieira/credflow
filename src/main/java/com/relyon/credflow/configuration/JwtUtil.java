package com.relyon.credflow.configuration;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION_MS;

    @PostConstruct
    public void validateConfiguration() {
        if (SECRET_KEY == null || SECRET_KEY.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set 'jwt.secret' property.");
        }

        if (SECRET_KEY.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long for security. Current length: " + SECRET_KEY.length()
            );
        }

        var weakSecrets = java.util.Set.of("local-test-secret", "change-me-please", "secret", "test");
        if (weakSecrets.contains(SECRET_KEY)) {
            throw new IllegalStateException(
                    "JWT secret is using a known weak value: '" + SECRET_KEY + "'. Please use a strong, unique secret."
            );
        }

        if (EXPIRATION_MS <= 0) {
            throw new IllegalStateException("JWT expiration must be greater than 0. Current value: " + EXPIRATION_MS);
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}