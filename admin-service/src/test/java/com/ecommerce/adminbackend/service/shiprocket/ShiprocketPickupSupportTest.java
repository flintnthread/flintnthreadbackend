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
    void resolvePincode_fromWarehousePinLabel() {
        assertEquals("500049", ShiprocketPickupSupport.resolvePincode(
                null,
                "F302 Perfect Towers\nPIN:500049",
                null
        ));
        assertEquals("500081", ShiprocketPickupSupport.resolvePincode(
                "500081",
                "Somewhere PIN:111111",
                null
        ));
    }
}
