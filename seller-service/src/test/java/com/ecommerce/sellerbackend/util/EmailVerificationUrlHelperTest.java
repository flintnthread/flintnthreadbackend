package com.ecommerce.sellerbackend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationUrlHelperTest {

    private EmailVerificationUrlHelper helper;

    @BeforeEach
    void setUp() {
        helper = new EmailVerificationUrlHelper();
        ReflectionTestUtils.setField(helper, "backendPublicUrl", "https://flintnthread.online");
        ReflectionTestUtils.setField(helper, "frontendBaseUrl", "https://flintnthread.online/Seller");
        ReflectionTestUtils.setField(helper, "emailVerifyRedirectUrl", "https://flintnthread.online/Seller/verify-email");
    }

    @Test
    void buildEmailLinkClickUrl_usesPublicDomain_notLocalhost() {
        String link = helper.buildEmailLinkClickUrl("abc123token");
        assertTrue(link.startsWith("https://flintnthread.online/api/auth/verify-email?token="));
        assertTrue(link.contains("abc123token"));
        assertFalse(link.toLowerCase().contains("localhost"));
        assertFalse(link.contains("127.0.0.1"));
        assertFalse(link.contains(":8081"));
        assertFalse(link.contains(":8083"));
    }

    @Test
    void buildLoginPageUrl_usesSellerFrontendPath() {
        String login = helper.buildLoginPageUrl("seller@example.com");
        assertTrue(login.startsWith("https://flintnthread.online/Seller/login?verified=1"));
        assertTrue(login.contains("seller%40example.com") || login.contains("seller@example.com"));
        assertFalse(login.toLowerCase().contains("localhost"));
    }
}
