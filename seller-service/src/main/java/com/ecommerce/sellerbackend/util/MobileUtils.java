package com.ecommerce.sellerbackend.util;

public final class MobileUtils {

    private MobileUtils() {
    }

    public static String normalizeDigits(String mobile) {
        if (mobile == null) {
            return "";
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    public static String toE164India(String mobile) {
        String digits = normalizeDigits(mobile);
        if (digits.length() != 10) {
            throw new IllegalArgumentException("Enter a valid 10-digit mobile number.");
        }
        return "+91" + digits;
    }

    public static String maskMobile(String mobile) {
        String digits = normalizeDigits(mobile);
        if (digits.length() < 4) {
            return "****";
        }
        return "******" + digits.substring(digits.length() - 4);
    }
}
