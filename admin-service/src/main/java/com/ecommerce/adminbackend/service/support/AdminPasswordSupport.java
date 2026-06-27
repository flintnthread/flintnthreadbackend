package com.ecommerce.adminbackend.service.support;

import org.springframework.security.crypto.password.PasswordEncoder;

public final class AdminPasswordSupport {

    private AdminPasswordSupport() {
    }

    /**
     * Supports bcrypt hashes and legacy plain-text passwords still stored in admin_users.
     */
    public static boolean matches(PasswordEncoder encoder, String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public static String encodeIfNeeded(PasswordEncoder encoder, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return rawPassword;
        }
        if (rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$") || rawPassword.startsWith("$2y$")) {
            return rawPassword;
        }
        return encoder.encode(rawPassword);
    }
}
