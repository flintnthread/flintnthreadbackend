package com.ecommerce.sellerbackend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-hours:24}") long expiryHours) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = expiryHours * 3_600_000L;
    }

    public String generateAccessToken(Long sellerId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(sellerId))
                .claim("email", email)
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
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public String refreshAccessToken(String existingToken) {
        Claims claims = parseClaims(existingToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired session."));
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            email = "seller@local";
        }
        return generateAccessToken(Long.parseLong(claims.getSubject()), email);
    }

    public Optional<Long> parseSellerId(String token) {
        return parseClaims(token).map(claims -> Long.parseLong(claims.getSubject()));
    }
}
