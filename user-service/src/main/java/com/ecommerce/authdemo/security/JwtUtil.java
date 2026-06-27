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

}