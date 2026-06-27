package com.ecommerce.sellerbackend.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductServiceImplTest {

    @Test
    void resolveDisplayStatus_returnsOutOfStockWhenNoStock() {
        assertEquals("Out of Stock", ProductServiceImpl.resolveDisplayStatus("active", 0));
    }

    @Test
    void resolveDisplayStatus_returnsInactiveForInactiveProduct() {
        assertEquals("Inactive", ProductServiceImpl.resolveDisplayStatus("inactive", 10));
        assertEquals("Inactive", ProductServiceImpl.resolveDisplayStatus("Draft", 5));
    }

    @Test
    void resolveDisplayStatus_returnsActiveWhenInStockAndActive() {
        assertEquals("Active", ProductServiceImpl.resolveDisplayStatus("approved", 12));
        assertEquals("Active", ProductServiceImpl.resolveDisplayStatus(null, 1));
    }
}
