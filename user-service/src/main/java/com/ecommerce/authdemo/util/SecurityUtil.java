package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Resolves the authenticated shopper for cart / wishlist / orders / addresses.
 * <p>
 * Ownership is always by {@code users.id}. When multiple accounts share the same
 * mobile number (Switch Account), the JWT {@code userId} claim selects the correct
 * row — never shipping phone and never an ambiguous {@code findByContactNumber}.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private static final String OTP_AUTH_PASSWORD_PLACEHOLDER = "OTP_AUTH_NOT_SET";

    private final UserRepository userRepository;
    private final Environment environment;

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {

            throw new RuntimeException("User not authenticated");
        }

        Object principalObj = authentication.getPrincipal();

        // Prefer JWT userId claim (switch-account safe)
        if (principalObj instanceof JwtPrincipal jwtPrincipal
                && jwtPrincipal.userId() != null
                && jwtPrincipal.userId() > 0) {
            Optional<User> byId = userRepository.findById(jwtPrincipal.userId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String principal = resolveIdentifier(principalObj);
        String normalized = principal.toLowerCase();

        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByEmail(normalized))
                // Only use contact number when JWT did not carry a userId
                .or(() -> {
                    if (principalObj instanceof JwtPrincipal jp && jp.userId() != null) {
                        return Optional.empty();
                    }
                    return userRepository.findByContactNumber(principal);
                })
                .or(() -> {
                    if (principalObj instanceof JwtPrincipal jp && jp.userId() != null) {
                        return Optional.empty();
                    }
                    return userRepository.findByContactNumber(normalized);
                })
                .orElseGet(() -> resolveMissingAuthenticatedUser(normalized));
    }

    private static String resolveIdentifier(Object principalObj) {
        if (principalObj instanceof JwtPrincipal jwtPrincipal) {
            return String.valueOf(jwtPrincipal.identifier() != null ? jwtPrincipal.identifier() : "").trim();
        }
        return String.valueOf(principalObj).trim();
    }

    private User resolveMissingAuthenticatedUser(String principal) {
        if (isDevProfile()) {
            return autoProvisionDevUser(principal);
        }
        throw new ResourceNotFoundException("User not found with identifier: " + principal);
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
    }

    private User autoProvisionDevUser(String identifier) {
        String normalized = String.valueOf(identifier == null ? "" : identifier).trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResourceNotFoundException("User not found with identifier: " + identifier);
        }

        User user = new User();
        if (normalized.contains("@")) {
            user.setEmail(normalized);
            user.setUsername(normalized);
        } else {
            user.setContactNumber(normalized);
            user.setUsername(normalized);
            user.setEmail(normalized + "@mobile.flintnthread.in");
        }
        user.setPassword(OTP_AUTH_PASSWORD_PLACEHOLDER);
        user.setVerified(true);
        user.setRole(Role.USER);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            return userRepository.findByEmail(normalized)
                    .or(() -> userRepository.findByContactNumber(normalized))
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found with identifier: " + identifier)
                    );
        }
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /** Returns authenticated user id when present, otherwise empty. */
    public Optional<Long> tryGetCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            Object principalObj = authentication.getPrincipal();
            if (principalObj instanceof JwtPrincipal jwtPrincipal
                    && jwtPrincipal.userId() != null
                    && jwtPrincipal.userId() > 0) {
                return Optional.of(jwtPrincipal.userId());
            }
            return Optional.of(getCurrentUser().getId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
