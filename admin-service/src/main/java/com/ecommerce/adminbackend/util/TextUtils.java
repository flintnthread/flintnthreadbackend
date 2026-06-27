package com.ecommerce.adminbackend.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + raw);
        }
    }
}
