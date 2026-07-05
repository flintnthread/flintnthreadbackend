package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CustomerAnalyticsAssemblerIT {

    @Autowired
    private CustomerAnalyticsAssembler assembler;

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    @Test
    void buildAnalyticsForCustomer95() {
        Optional<Object[]> row = customerQueryRepository.findCustomerById(95L);
        assertNotNull(row.orElseThrow());
        Map<String, Object> result = assembler.build(95L, row.get());
        assertNotNull(result.get("totalOrders"));
    }

    @Test
    void recentOrdersIncludeProductNameForCustomer95() {
        Optional<Object[]> row = customerQueryRepository.findCustomerById(95L);
        assertTrue(row.isPresent(), "Customer 95 must exist in test database");

        Map<String, Object> result = assembler.build(95L, row.get());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertNotNull(recentOrders);
        assertFalse(recentOrders.isEmpty(), "Customer 95 should have recent orders");

        for (Map<String, Object> order : recentOrders) {
            assertNotNull(order.get("id"), "Recent order must include numeric order id");
            assertNotNull(order.get("orderId"), "Recent order must include orderId");
            String productName = String.valueOf(order.get("productName"));
            assertFalse(productName.isBlank(), "productName must be populated for order " + order.get("id"));
            assertFalse("Product".equals(productName),
                    "productName should resolve from order items or catalog for order " + order.get("id"));
        }
    }
}
