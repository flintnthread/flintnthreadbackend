package com.ecommerce.sellerbackend.service.support;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ProductSkuGenerator {

    private ProductSkuGenerator() {}

    /** Format: ABC-BLM1-4829 — name(3)-color(2)+size(2)-random(4) */
    public static String generateVariantSku(String productName, String color, String size) {
        String namePart = token(productName, 3);
        String colorPart = token(color, 2);
        String sizePart = token(size, 2);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return namePart + "-" + colorPart + sizePart + "-" + random;
    }

    private static String token(String raw, int maxLen) {
        String clean = raw == null ? "" : raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
        if (clean.isEmpty()) {
            return "X".repeat(maxLen);
        }
        if (clean.length() >= maxLen) {
            return clean.substring(0, maxLen);
        }
        return clean + "X".repeat(maxLen - clean.length());
    }
}
