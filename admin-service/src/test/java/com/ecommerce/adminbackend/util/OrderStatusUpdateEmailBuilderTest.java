package com.ecommerce.adminbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusUpdateEmailBuilderTest {

    @Test
    void buildHtmlIncludesOrderStatusAndComment() {
        String html = OrderStatusUpdateEmailBuilder.buildHtml(
                "Jane Doe",
                "#ORD-1001",
                "Processing",
                "Your package is on the way.",
                "support@flintnthread.in",
                "+91 9063499092");

        assertTrue(html.contains("Jane Doe"));
        assertTrue(html.contains("#ORD-1001"));
        assertTrue(html.contains("Processing"));
        assertTrue(html.contains("Your package is on the way."));
    }

    @Test
    void buildSubjectUsesOrderNumber() {
        String subject = OrderStatusUpdateEmailBuilder.buildSubject("#ORD-1001");
        assertTrue(subject.contains("#ORD-1001"));
    }
}
