package com.ecommerce.authdemo.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * JWT principal for switch-account: keeps the login identifier (email/mobile)
 * while binding ownership to the exact {@code userId} claim so multiple accounts
 * that share a phone number do not mix cart / orders / addresses.
 */
public record JwtPrincipal(String identifier, Long userId) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return identifier != null ? identifier : "";
    }
}
