package com.ecommerce.sellerbackend.security;

import com.ecommerce.sellerbackend.exception.ForbiddenException;
import com.ecommerce.sellerbackend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated seller id from JWT (source of truth).
 * Rejects requests where X-Seller-Id does not match the logged-in seller.
 */
@Component
public class AuthenticatedSellerResolver {

    public Long requireCurrentSellerId(Long headerSellerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long jwtSellerId) {
            if (headerSellerId != null && headerSellerId > 0 && !headerSellerId.equals(jwtSellerId)) {
                throw new ForbiddenException("You can only access your own seller account.");
            }
            return jwtSellerId;
        }

        if (headerSellerId == null || headerSellerId <= 0) {
            throw new UnauthorizedException("Valid seller session is required.");
        }
        return headerSellerId;
    }
}
