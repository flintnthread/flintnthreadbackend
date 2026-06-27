package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

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

        String principal = String.valueOf(authentication.getName()).trim();
        String normalized = principal.toLowerCase();

        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByEmail(normalized))
                .or(() -> userRepository.findByContactNumber(principal))
                .or(() -> userRepository.findByContactNumber(normalized))
                .orElseGet(() -> resolveMissingAuthenticatedUser(normalized));
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
            return Optional.of(getCurrentUser().getId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}