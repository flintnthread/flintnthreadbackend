package com.ecommerce.adminbackend.service.shiprocket;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers for Shiprocket seller pickup nicknames / warehouse address.
 * Pickup must always be the product's seller — never platform Ashvi/ASVI/work defaults.
 */
public final class ShiprocketPickupSupport {

    private static final Pattern SIX_DIGIT_PIN = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
    private static final Pattern PIN_LABEL = Pattern.compile("PIN\\s*:\\s*(\\d{6})", Pattern.CASE_INSENSITIVE);

    private ShiprocketPickupSupport() {
    }

    /**
     * Prefer the catalog product seller (source of truth for which warehouse ships the item).
     */
    public static Long resolveSellerId(Long productSellerId, Long orderItemSellerId) {
        if (productSellerId != null && productSellerId > 0) {
            return productSellerId;
        }
        if (orderItemSellerId != null && orderItemSellerId > 0) {
            return orderItemSellerId;
        }
        return null;
    }

    /**
     * Stable Shiprocket pickup nickname unique per seller.
     * Format: S{id}-{BusinessName} (max 36 chars). Never returns work/ASVI/Ashvi platform defaults.
     */
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

    /**
     * Build Shiprocket pickup address from the product seller profile.
     * Prefers warehouse fields; falls back to business address only when warehouse is missing.
     */
    public static SellerPickupAddress buildSellerPickupAddress(
            String warehouseAddressRaw,
            String warehouseArea,
            String warehouseCity,
            String warehouseState,
            String warehouseCountry,
            String businessAddress,
            String businessArea,
            String businessCity,
            String businessState,
            String businessCountry,
            String businessPincode
    ) {
        boolean hasWarehouse = hasText(warehouseAddressRaw)
                || hasText(warehouseCity)
                || hasText(warehouseState);

        String street;
        String landmark;
        String area;
        String city;
        String state;
        String country;
        String pin;

        if (hasWarehouse) {
            street = extractWarehouseStreet(warehouseAddressRaw);
            landmark = extractWarehouseLandmark(warehouseAddressRaw);
            area = trimToNull(warehouseArea);
            city = firstNonBlank(warehouseCity, businessCity);
            state = firstNonBlank(warehouseState, businessState);
            country = firstNonBlank(warehouseCountry, businessCountry, "India");
            // Warehouse PIN first — never prefer business PIN over warehouse.
            pin = firstSixDigitPin(
                    extractPin(warehouseAddressRaw),
                    digitsOnly(businessPincode),
                    extractPin(businessAddress)
            );
            if (!hasText(street)) {
                street = extractWarehouseStreet(businessAddress);
            }
            if (!hasText(street)) {
                street = trimToNull(businessAddress);
            }
        } else {
            street = extractWarehouseStreet(businessAddress);
            if (!hasText(street)) {
                street = trimToNull(businessAddress);
            }
            landmark = extractWarehouseLandmark(businessAddress);
            area = trimToNull(businessArea);
            city = trimToNull(businessCity);
            state = trimToNull(businessState);
            country = firstNonBlank(businessCountry, "India");
            pin = firstSixDigitPin(
                    digitsOnly(businessPincode),
                    extractPin(businessAddress)
            );
        }

        if (hasText(landmark) && !hasText(area)) {
            area = landmark;
            landmark = null;
        } else if (hasText(landmark) && hasText(area) && !area.equalsIgnoreCase(landmark)) {
            area = area + ", " + landmark;
        }

        return new SellerPickupAddress(
                trimToNull(street),
                trimToNull(area),
                trimToNull(city),
                trimToNull(state),
                trimToNull(country),
                pin != null ? pin : ""
        );
    }

    public static String extractWarehouseStreet(String warehouseAddress) {
        if (warehouseAddress == null || warehouseAddress.isBlank()) {
            return null;
        }
        int landmarkIdx = indexOfIgnoreCase(warehouseAddress, "\nLandmark:");
        int pinIdx = indexOfIgnoreCase(warehouseAddress, "\nPIN:");
        int end = warehouseAddress.length();
        if (landmarkIdx >= 0) {
            end = Math.min(end, landmarkIdx);
        }
        if (pinIdx >= 0) {
            end = Math.min(end, pinIdx);
        }
        String street = warehouseAddress.substring(0, end).trim();
        return street.isEmpty() ? null : street.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    public static String extractWarehouseLandmark(String warehouseAddress) {
        if (warehouseAddress == null) {
            return null;
        }
        for (String line : warehouseAddress.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "Landmark:", 0, "Landmark:".length())) {
                String value = trimmed.substring("Landmark:".length()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
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

    /** @deprecated use {@link #buildSellerPickupAddress} — kept for older call sites/tests */
    @Deprecated
    public static String resolvePincode(String sellerPincode, String warehouseAddress, String businessAddress) {
        return firstSixDigitPin(
                extractPin(warehouseAddress),
                digitsOnly(sellerPincode),
                extractPin(businessAddress)
        );
    }

    public record SellerPickupAddress(
            String street,
            String address2,
            String city,
            String state,
            String country,
            String pincode
    ) {
        public boolean isComplete() {
            return hasText(street)
                    && hasText(city)
                    && hasText(state)
                    && pincode != null
                    && pincode.length() == 6;
        }
    }

    private static String firstSixDigitPin(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String digits = digitsOnly(value);
            if (digits.length() == 6) {
                return digits;
            }
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String sanitizeBusiness(String businessName) {
        if (businessName == null) {
            return "";
        }
        String cleaned = businessName.trim()
                .replaceAll("[^A-Za-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.replace(' ', '-');
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ENGLISH).indexOf(needle.toLowerCase(Locale.ENGLISH));
    }
}
