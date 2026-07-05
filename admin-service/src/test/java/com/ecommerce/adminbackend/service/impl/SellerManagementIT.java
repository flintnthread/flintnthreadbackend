package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.SellerAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.mail.dev-mode=true")
class SellerManagementIT {

    @Autowired
    private SellerAdminService sellerAdminService;

    @Test
    void listSellers_withoutStatus_returnsAllIncludingEmailPending() {
        PageResponse<Map<String, Object>> page = sellerAdminService.listSellers(null, null, 0, 50);
        assertNotNull(page);
        assertNotNull(page.items());
        assertFalse(page.items().isEmpty(), "Expected sellers in database");

        boolean hasEmailPending = page.items().stream()
                .anyMatch(row -> "email_pending".equals(String.valueOf(row.get("status"))));
        assertTrue(hasEmailPending, "Expected at least one email_pending seller in all-sellers list");

        page.items().stream()
                .filter(row -> "email_pending".equals(String.valueOf(row.get("status"))))
                .forEach(row -> assertTrue(
                        Boolean.TRUE.equals(row.get("resendVerificationEligible")),
                        "email_pending seller should be eligible for resend: id=" + row.get("id")));
    }

    @Test
    void resendEmailVerification_forEmailPendingSeller_succeeds() {
        PageResponse<Map<String, Object>> page = sellerAdminService.listSellers("email_pending", null, 0, 1);
        assertFalse(page.items().isEmpty(), "Need at least one email_pending seller");

        Long sellerId = ((Number) page.items().get(0).get("id")).longValue();
        Map<String, Object> result = sellerAdminService.resendEmailVerification(sellerId);

        assertNotNull(result.get("message"));
        assertTrue(result.get("message").toString().toLowerCase().contains("verification"));
    }

    @Test
    void resendEmailVerification_forVerifiedSeller_fails() {
        PageResponse<Map<String, Object>> page = sellerAdminService.listSellers("active", null, 0, 1);
        assertFalse(page.items().isEmpty(), "Need at least one active seller");

        Long sellerId = ((Number) page.items().get(0).get("id")).longValue();
        try {
            sellerAdminService.resendEmailVerification(sellerId);
            throw new AssertionError("Expected resend to fail for verified/active seller");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("verified"));
        }
    }
}
