package com.app.backend.security;

import com.app.backend.config.JwtProperties;
import com.app.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getExpirationSeconds() * 1000L);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getName());

        if (jwtProperties.getIssuer() != null && !jwtProperties.getIssuer().isBlank()) {
            builder.issuer(jwtProperties.getIssuer());
        }
        if (jwtProperties.getAudience() != null && !jwtProperties.getAudience().isBlank()) {
            builder.audience().add(jwtProperties.getAudience()).and();
        }

        return builder.signWith(key).compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
