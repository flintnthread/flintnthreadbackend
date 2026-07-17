package com.ecommerce.authdemo.security;

import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilUserIdClaimTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void generateToken_includesUserIdClaim() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user1@gmail.com");
        user.setContactNumber("9876543210");
        user.setRole(Role.USER);

        String token = jwtUtil.generateToken(user);
        assertEquals(42L, jwtUtil.extractUserId(token));
        assertEquals("user1@gmail.com", jwtUtil.extractIdentifier(token));
    }

    @Test
    void generateToken_withPhoneSubject_stillCarriesDistinctUserId() {
        String tokenA = jwtUtil.generateToken("9876543210", "USER", 101L);
        String tokenB = jwtUtil.generateToken("9876543210", "USER", 202L);

        assertEquals("9876543210", jwtUtil.extractIdentifier(tokenA));
        assertEquals("9876543210", jwtUtil.extractIdentifier(tokenB));
        assertEquals(101L, jwtUtil.extractUserId(tokenA));
        assertEquals(202L, jwtUtil.extractUserId(tokenB));
    }

    @Test
    void extractUserId_missingClaimReturnsNull() {
        String token = jwtUtil.generateToken("only-subject@example.com", "USER");
        assertNull(jwtUtil.extractUserId(token));
        assertNotNull(jwtUtil.extractIdentifier(token));
    }
}
