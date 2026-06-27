package com.ecommerce.authdemo.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.util.Date;

    @Component
    public class JwtTokenProvider {

        private final String SECRET_KEY = "secret-key-demo";

        private final long EXPIRATION_TIME = 86400000;

        public String generateToken(String username) {

            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                    .compact();
        }

        public String getUsernameFromToken(String token) {

            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        }

        public boolean validateToken(String token) {

            try {
                Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
                return true;

            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }
    }

