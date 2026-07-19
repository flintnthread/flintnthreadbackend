package com.ecommerce.sellerbackend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationUrlHelperTest {

    private EmailVerificationUrlHelper helper;

    @BeforeEach
    void setUp() {
        helper = new EmailVerificationUrlHelper();
        ReflectionTestUtils.setField(helper, "backendPublicUrl", "https://flintnthread.in");
        ReflectionTestUtils.setField(helper, "frontendBaseUrl", "https://seller.flintnthread.in");
        ReflectionTestUtils.setField(helper, "emailVerifyRedirectUrl", "https://seller.flintnthread.in/verify-email");
    }

    @Test
    void buildEmailLinkClickUrl_usesSslSafeDomain_notLocalhost() {
        String link = helper.buildEmailLinkClickUrl("abc123token");
        assertTrue(link.startsWith("https://flintnthread.in/api/auth/verify-email?token="));
        assertTrue(link.contains("abc123token"));
        assertFalse(link.toLowerCase().contains("localhost"));
        assertFalse(link.contains("127.0.0.1"));
        assertFalse(link.contains(":8081"));
        assertFalse(link.contains(":8083"));
        assertFalse(link.contains("flintnthread.online"));
    }

    @Test
    void buildEmailLinkClickUrl_rewritesOnlineHost_toMatchTlsCertificate() {
        ReflectionTestUtils.setField(helper, "backendPublicUrl", "https://flintnthread.online");
        ReflectionTestUtils.setField(helper, "frontendBaseUrl", "https://seller.flintnthread.online");
        ReflectionTestUtils.setField(helper, "emailVerifyRedirectUrl", "https://seller.flintnthread.online/verify-email");

        String link = helper.buildEmailLinkClickUrl("tok");
        assertEquals("https://flintnthread.in/api/auth/verify-email?token=tok", link);

        String otp = helper.buildOtpPageRedirect("a@b.com", true, false);
        assertTrue(otp.startsWith("https://seller.flintnthread.in/verify-email?email="));
        assertFalse(otp.contains("flintnthread.online"));
    }

    @Test
    void buildOtpPageRedirect_rewritesLegacySellerPath() {
        ReflectionTestUtils.setField(helper, "frontendBaseUrl", "https://flintnthread.in/Seller");
        ReflectionTestUtils.setField(helper, "emailVerifyRedirectUrl", "https://flintnthread.in/Seller/verify-email");

        String otp = helper.buildOtpPageRedirect("a@b.com", true, false);
        assertTrue(otp.startsWith("https://seller.flintnthread.in/verify-email?email="));
        assertFalse(otp.contains("/Seller/"));
    }

    @Test
    void buildLoginPageUrl_usesSellerSubdomain() {
        String login = helper.buildLoginPageUrl("seller@example.com");
        assertTrue(login.startsWith("https://seller.flintnthread.in/login?verified=1"));
        assertTrue(login.contains("seller%40example.com") || login.contains("seller@example.com"));
        assertFalse(login.toLowerCase().contains("localhost"));
        assertFalse(login.contains("flintnthread.online"));
        assertFalse(login.contains("/Seller/"));
    }
}
