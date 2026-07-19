package com.ecommerce.sellerbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SslSafePublicUrlTest {

    @Test
    void normalize_rewritesOnlineHostsToIn() {
        assertEquals(
                "https://flintnthread.in/api/auth/verify-email?token=x",
                SslSafePublicUrl.normalize("https://flintnthread.online/api/auth/verify-email?token=x"));
        assertEquals(
                "https://flintnthread.in/Seller/verify-email",
                SslSafePublicUrl.normalize("https://flintnthread.online/Seller/verify-email"));
        assertEquals(
                "https://seller.flintnthread.in/path",
                SslSafePublicUrl.normalize("https://seller.flintnthread.online/path"));
        assertEquals(
                "https://flintnthread.in",
                SslSafePublicUrl.normalize("https://flintnthread.in"));
    }
}
