package com.ecommerce.authdemo.security;

import com.ecommerce.authdemo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
            "mysecretkeymysecretkeymysecretkeymysecretkey12345";

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours



    /*
     ------------------------------------------------
     TOKEN GENERATION FOR USER ENTITY
     ------------------------------------------------
     This method is used when login comes from USER table
     */
    public String generateToken(User user) {

        String identifier;

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            identifier = user.getEmail();
        } else {
            identifier = user.getContactNumber();
        }

        return Jwts.builder()
                .setSubject(identifier)
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }



    /*
     ------------------------------------------------
     GENERIC TOKEN GENERATION (ADMIN / SELLER / USER)
     ------------------------------------------------
     This method allows token creation for ANY ROLE
     */
    /**
     * Same as {@link #generateToken(String, String, Long)} with no numeric user id
     * (older clients only).
     */
    public String generateToken(String identifier, String role) {
        return generateToken(identifier, role, null);
    }

    /**
     * Mobile apps decode {@code userId} from the JWT; subject stays email/mobile string.
     */
    public String generateToken(String identifier, String role, Long userId) {

        var builder = Jwts.builder()
                .setSubject(identifier)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME));

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }



    /*
     ------------------------------------------------
     EXTRACT ALL CLAIMS
     ------------------------------------------------
     */
    public Claims extractClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }



    /*
     ------------------------------------------------
     EXTRACT EMAIL OR MOBILE
     ------------------------------------------------
     */
    public String extractIdentifier(String token) {

        return extractClaims(token).getSubject();
    }



    /*
     ------------------------------------------------
     EXTRACT ROLE
     ------------------------------------------------
     */
    public String extractRole(String token) {

        return extractClaims(token).get("role", String.class);
    }

    /**
     * Numeric account id from JWT {@code userId} claim (set at login).
     * Prefer this over email/phone lookup when multiple users share a contact number.
     */
    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Object raw = extractClaims(token).get("userId");
            if (raw == null) {
                return null;
            }
            if (raw instanceof Number n) {
                long id = n.longValue();
                return id > 0 ? id : null;
            }
            String text = String.valueOf(raw).trim();
            if (text.isEmpty()) {
                return null;
            }
            long id = Long.parseLong(text);
            return id > 0 ? id : null;
        } catch (Exception e) {
            return null;
        }
    }



    /*
     ------------------------------------------------
     CHECK TOKEN EXPIRATION
     ------------------------------------------------
     */
    public boolean isTokenExpired(String token) {

        Date expiry = extractClaims(token).getExpiration();

        return expiry.before(new Date());
    }



    /*
     ------------------------------------------------
     VALIDATE TOKEN
     ------------------------------------------------
     */
    public boolean validateToken(String token, String identifier) {

        String tokenIdentifier = extractIdentifier(token);

        return tokenIdentifier.equals(identifier) && !isTokenExpired(token);
    }

    /**
     * Short-lived proof that signup email OTP was verified (30 minutes).
     */
    public String generateSignupEmailToken(String email) {
        return Jwts.builder()
                .setSubject(email.trim().toLowerCase())
                .claim("purpose", "SIGNUP_EMAIL_VERIFIED")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 30))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isValidSignupEmailToken(String token, String email) {
        if (token == null || token.isBlank() || email == null || email.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractClaims(token);
            String purpose = claims.get("purpose", String.class);
            String subject = claims.getSubject();
            return "SIGNUP_EMAIL_VERIFIED".equals(purpose)
                    && email.trim().equalsIgnoreCase(subject)
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}