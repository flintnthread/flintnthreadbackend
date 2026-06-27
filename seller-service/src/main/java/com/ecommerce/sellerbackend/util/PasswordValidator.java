package com.ecommerce.sellerbackend.util;

public final class PasswordValidator {

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password must not exceed 100 characters.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter.");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one number.");
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character.");
        }
    }
}
