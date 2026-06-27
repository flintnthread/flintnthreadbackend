package com.ecommerce.adminbackend.security;

import com.ecommerce.adminbackend.logging.LogFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class AdminJwtService {

    private static final Logger log = LogFactory.getLogger(AdminJwtService.class);

    private final SecretKey signingKey;
    private final long expiryMillis;

    public AdminJwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-hours:8}") long expiryHours) {
        if (secret == null || secret.length() < 32) {
            log.error("JWT secret is missing or too short (min 32 chars).");
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = expiryHours * 3_600_000L;
    }

    public String generateAccessToken(Long adminId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("email", email)
                .claim("role", role)
                .claim("type", "admin")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirySeconds() {
        return expiryMillis / 1000L;
    }

    public Optional<Claims> parseClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            if (!"admin".equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception ex) {
            log.debug("JWT parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Long> parseAdminId(String token) {
        return parseClaims(token).map(claims -> Long.parseLong(claims.getSubject()));
    }
}
