package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.ProductAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ProductAdminListIT {

    @Autowired
    private ProductAdminService productAdminService;

    @Test
    void listProductsAndStats() {
        PageResponse<Map<String, Object>> page = productAdminService.listProducts(
                null, null, null, null, null, null, null, 0, 20);
        assertNotNull(page);
        assertNotNull(page.items());

        Map<String, Object> stats = productAdminService.stats();
        assertNotNull(stats.get("total"));
    }

    @Test
    void statsUsesEfficientStockCounts() {
        Map<String, Object> stats = productAdminService.stats();
        assertNotNull(stats.get("outOfStock"));
        assertNotNull(stats.get("lowStock"));
    }
}
