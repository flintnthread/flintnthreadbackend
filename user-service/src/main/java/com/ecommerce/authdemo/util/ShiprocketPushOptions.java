package com.ecommerce.authdemo.util;

import java.util.List;

/**
 * Optional scope when pushing a multi-seller order to Shiprocket.
 * When set, only matching seller line items are shipped and priced.
 */
public record ShiprocketPushOptions(Long sellerId) {

    public static ShiprocketPushOptions empty() {
        return new ShiprocketPushOptions(null);
    }

    public static ShiprocketPushOptions forSeller(Long sellerId) {
        return new ShiprocketPushOptions(sellerId);
    }

    public boolean isScoped() {
        return sellerId != null;
    }
}
