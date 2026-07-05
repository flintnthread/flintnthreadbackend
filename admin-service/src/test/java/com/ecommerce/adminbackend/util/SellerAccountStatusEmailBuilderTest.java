package com.ecommerce.adminbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerAccountStatusEmailBuilderTest {

    @Test
    void buildHtml_includesSellerNameStatusAndReason() {
        String html = SellerAccountStatusEmailBuilder.buildHtml(
                "Janu Sk",
                "Inactive",
                "Incomplete KYC documents",
                "support@flintnthread.in",
                "+91 9063499092");

        assertTrue(html.contains("Janu Sk"));
        assertTrue(html.contains("Inactive"));
        assertTrue(html.contains("Incomplete KYC documents"));
        assertTrue(html.contains("support@flintnthread.in"));
    }

    @Test
    void buildSubject_containsStatusLabel() {
        String subject = SellerAccountStatusEmailBuilder.buildSubject("Inactive");
        assertTrue(subject.contains("Inactive"));
    }
}
