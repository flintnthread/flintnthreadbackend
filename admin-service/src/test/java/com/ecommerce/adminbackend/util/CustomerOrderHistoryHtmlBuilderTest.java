package com.ecommerce.adminbackend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerOrderHistoryHtmlBuilderTest {

    @Test
    void buildIncludesCustomerAndOrderRows() {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", 95L);
        customer.put("name", "Test Customer");
        customer.put("orderCount", 2L);
        customer.put("totalSpent", new BigDecimal("1500.50"));
        customer.put("lastOrderAt", LocalDateTime.of(2026, 3, 1, 10, 0));

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNumber", "ORD-1001");
        order.put("createdAt", LocalDateTime.of(2026, 3, 1, 10, 0));
        order.put("itemCount", 3L);
        order.put("totalAmount", new BigDecimal("750.25"));
        order.put("paymentMethod", "cod");
        order.put("orderStatus", "delivered");

        String html = CustomerOrderHistoryHtmlBuilder.build(customer, List.of(order));

        assertTrue(html.contains("Test Customer"));
        assertTrue(html.contains("ORD-1001"));
        assertTrue(html.contains("₹750.25"));
        assertTrue(html.contains("Customer Order History"));
    }
}
