package com.ecommerce.authdemo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiprocketPickupSupportTest {

    @Test
    void resolveSellerId_prefersProductSeller() {
        assertEquals(88L, ShiprocketPickupSupport.resolveSellerId(88L, 11L));
        assertEquals(11L, ShiprocketPickupSupport.resolveSellerId(null, 11L));
    }

    @Test
    void buildSellerPickupAddress_fromProductSellerWarehouse() {
        var addr = ShiprocketPickupSupport.buildSellerPickupAddress(
                "12 Market Road\nLandmark: Temple\nPIN:560001",
                "Indiranagar",
                "Bengaluru",
                "Karnataka",
                "India",
                "Business only",
                null,
                "Chennai",
                "TN",
                "India",
                "600001"
        );
        assertEquals("12 Market Road", addr.street());
        assertEquals("Bengaluru", addr.city());
        assertEquals("560001", addr.pincode());
        assertTrue(addr.isComplete());
    }

    @Test
    void pickupNickname_unique() {
        assertEquals("S42-My-Store", ShiprocketPickupSupport.pickupNickname(42L, "My Store"));
    }
}
