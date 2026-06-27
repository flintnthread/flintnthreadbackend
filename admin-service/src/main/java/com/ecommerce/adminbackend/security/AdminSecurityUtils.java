package com.ecommerce.adminbackend.security;

import com.ecommerce.adminbackend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AdminSecurityUtils {

    private AdminSecurityUtils() {
    }

    public static Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long adminId) {
            return adminId;
        }
        throw new UnauthorizedException("Admin session required.");
    }
}
