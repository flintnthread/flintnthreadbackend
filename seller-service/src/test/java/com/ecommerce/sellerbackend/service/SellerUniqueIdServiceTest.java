package com.ecommerce.sellerbackend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellerUniqueIdServiceTest {

    @Test
    void formatUniqueId_usesFntSellerPrefixWithSixDigitPadding() {
        assertEquals("FNT-SELLER-000086", SellerUniqueIdService.formatUniqueId(86L));
        assertEquals("FNT-SELLER-000294", SellerUniqueIdService.formatUniqueId(294L));
        assertEquals("FNT-SELLER-000248", SellerUniqueIdService.formatUniqueId(248L));
    }
}
