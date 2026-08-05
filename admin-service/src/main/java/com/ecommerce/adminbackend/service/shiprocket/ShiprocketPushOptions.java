package com.ecommerce.adminbackend.service.shiprocket;

import java.util.List;
import java.util.Locale;

/**
 * Optional scope when pushing a multi-seller order to Shiprocket from admin.
 * When set, only matching line items are shipped and priced for that seller.
 */
public record ShiprocketPushOptions(
        Long sellerId,
        List<Long> productIds,
        String sellerName
) {
    public static ShiprocketPushOptions empty() {
        return new ShiprocketPushOptions(null, List.of(), null);
    }

    public boolean isScoped() {
        return sellerId != null
                || (productIds != null && !productIds.isEmpty())
                || (sellerName != null && !sellerName.isBlank());
    }

    public List<Long> productIds() {
        return productIds != null ? productIds : List.of();
    }

    public String normalizedSellerName() {
        if (sellerName == null || sellerName.isBlank()) {
            return null;
        }
        return sellerName.trim().toLowerCase(Locale.ENGLISH);
    }

    public static ShiprocketPushOptions of(Long sellerId, List<Long> productIds, String sellerName) {
        if (sellerId == null
                && (productIds == null || productIds.isEmpty())
                && (sellerName == null || sellerName.isBlank())) {
            return empty();
        }
        return new ShiprocketPushOptions(
                sellerId,
                productIds != null ? List.copyOf(productIds) : List.of(),
                sellerName
        );
    }
}
