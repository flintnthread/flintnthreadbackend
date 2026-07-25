package com.ecommerce.authdemo.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shiprocket pickup helpers — product seller warehouse only (never Ashvi/work defaults).
 */
public final class ShiprocketPickupSupport {

    private static final Pattern SIX_DIGIT_PIN = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
    private static final Pattern PIN_LABEL = Pattern.compile("PIN\\s*:\\s*(\\d{6})", Pattern.CASE_INSENSITIVE);

    private ShiprocketPickupSupport() {
    }

    public static Long resolveSellerId(Long productSellerId, Long orderItemSellerId) {
        if (productSellerId != null && productSellerId > 0) {
            return productSellerId;
        }
        if (orderItemSellerId != null && orderItemSellerId > 0) {
            return orderItemSellerId;
        }
        return null;
    }

    public static String pickupNickname(Long sellerId, String businessName) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("sellerId is required for Shiprocket pickup");
        }
        String prefix = "S" + sellerId + "-";
        String biz = sanitizeBusiness(businessName);
        if (biz.isEmpty() || isForbiddenPlatformPickup(biz)) {
            biz = "Seller";
        }
        int remaining = 36 - prefix.length();
        if (remaining < 1) {
            String fallback = "S" + sellerId;
            return fallback.length() <= 36 ? fallback : fallback.substring(0, 36);
        }
        if (biz.length() > remaining) {
            biz = biz.substring(0, remaining).replaceAll("[-_]+$", "").trim();
            if (biz.isEmpty()) {
                biz = "Seller";
                if (biz.length() > remaining) {
                    biz = biz.substring(0, remaining);
                }
            }
        }
        return prefix + biz;
    }

    public static boolean isForbiddenPlatformPickup(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return true;
        }
        String n = nickname.trim().toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return n.equals("work")
                || n.equals("ashvi homes")
                || n.equals("ashvi home")
                || n.equals("asvi homes")
                || n.equals("asvi home")
                || n.equals("asvi home foods")
                || n.equals("ashvi home foods")
                || n.equals("asvi")
                || n.equals("ashvi");
    }

    public static String resolvePincode(String sellerPincode, String warehouseAddress, String businessAddress) {
        String fromSeller = digitsOnly(sellerPincode);
        if (fromSeller.length() == 6) {
            return fromSeller;
        }
        String fromWarehouse = extractPin(warehouseAddress);
        if (fromWarehouse.length() == 6) {
            return fromWarehouse;
        }
        String fromBusiness = extractPin(businessAddress);
        if (fromBusiness.length() == 6) {
            return fromBusiness;
        }
        return fromSeller;
    }

    public static String extractPin(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher labeled = PIN_LABEL.matcher(text);
        if (labeled.find()) {
            return labeled.group(1);
        }
        Matcher six = SIX_DIGIT_PIN.matcher(text);
        String last = "";
        while (six.find()) {
            last = six.group(1);
        }
        return last;
    }

    private static String sanitizeBusiness(String businessName) {
        if (businessName == null) {
            return "";
        }
        return businessName.trim()
                .replaceAll("[^A-Za-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replace(' ', '-');
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }
}
