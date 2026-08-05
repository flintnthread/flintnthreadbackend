package com.ecommerce.adminbackend.service.shiprocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiprocketPickupSupportTest {

    @Test
    void resolveSellerId_prefersProductSeller() {
        assertEquals(88L, ShiprocketPickupSupport.resolveSellerId(88L, 11L));
        assertEquals(11L, ShiprocketPickupSupport.resolveSellerId(null, 11L));
        assertEquals(null, ShiprocketPickupSupport.resolveSellerId(null, null));
    }

    @Test
    void pickupNickname_isUniquePerSeller_andNeverBareAshviOrWork() {
        String nick = ShiprocketPickupSupport.pickupNickname(266L, "Flint Thread Store");
        assertEquals("S266-Flint-Thread-Store", nick);
        assertFalse(ShiprocketPickupSupport.isForbiddenPlatformPickup(nick));

        String ashviBiz = ShiprocketPickupSupport.pickupNickname(10L, "Ashvi Homes");
        assertEquals("S10-Seller", ashviBiz);

        String workBiz = ShiprocketPickupSupport.pickupNickname(5L, "work");
        assertEquals("S5-Seller", workBiz);
    }

    @Test
    void buildSellerPickupAddress_usesWarehouseNotBusinessPin() {
        var addr = ShiprocketPickupSupport.buildSellerPickupAddress(
                "F302 Perfect Towers Miyapur\nLandmark: Near Ambedkar\nPIN:500049",
                "Hafeezpet",
                "Hyderabad",
                "Telangana",
                "India",
                "Old Business Street",
                "Old Area",
                "Secunderabad",
                "Telangana",
                "India",
                "500003"
        );

        assertEquals("F302 Perfect Towers Miyapur", addr.street());
        assertTrue(addr.address2().contains("Hafeezpet"));
        assertTrue(addr.address2().contains("Near Ambedkar"));
        assertEquals("Hyderabad", addr.city());
        assertEquals("Telangana", addr.state());
        assertEquals("500049", addr.pincode());
        assertTrue(addr.isComplete());
    }

    @Test
    void resolvePincode_prefersWarehousePinLabel() {
        assertEquals("500049", ShiprocketPickupSupport.resolvePincode(
                "500003",
                "F302 Perfect Towers\nPIN:500049",
                null
        ));
        assertEquals("500081", ShiprocketPickupSupport.resolvePincode(
                "500081",
                null,
                "Somewhere"
        ));
    }
}
