package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.ForgotPasswordRequest;
import com.ecommerce.sellerbackend.dto.MessageResponse;
import com.ecommerce.sellerbackend.dto.ResetPasswordRequest;
import com.ecommerce.sellerbackend.dto.ResetTokenValidationResponse;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.ForbiddenException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.service.PasswordResetService;
import com.ecommerce.sellerbackend.util.PasswordResetUrlHelper;
import com.ecommerce.sellerbackend.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordResetUrlHelper passwordResetUrlHelper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.password-reset-expiry-hours:1}")
    private int resetExpiryHours;

    @Override
    @Transactional
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Seller seller = sellerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No seller account found with this email. Please register first or check the email you used during signup."));

        assertCanResetPassword(seller);
        String token = generateResetToken();
        seller.setPasswordResetToken(token);
        seller.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(resetExpiryHours));
        sellerRepository.save(seller);

        String recipientName = buildDisplayName(seller);
        String resetLink = passwordResetUrlHelper.buildEmailLinkClickUrl(token);
        mailService.sendPasswordResetEmail(seller.getEmail(), recipientName, resetLink);
        log.info("Password reset email sent to {}", maskEmail(seller.getEmail()));

        return new MessageResponse(
                "A password reset link has been sent to your registered email. Please check your inbox and spam folder. "
                        + "The link expires in 1 hour.");
    }

    @Override
    public ResetTokenValidationResponse validateResetToken(String token) {
        String normalized = normalizeToken(token);
        if (normalized.isBlank()) {
            return ResetTokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or expired reset link.")
                    .build();
        }

        Optional<Seller> sellerOpt = sellerRepository.findByPasswordResetToken(normalized);
        if (sellerOpt.isEmpty() || isTokenExpired(sellerOpt.get())) {
            return ResetTokenValidationResponse.builder()
                    .valid(false)
                    .message("This reset link is invalid or has expired. Please request a new one.")
                    .build();
        }

        Seller seller = sellerOpt.get();
        return ResetTokenValidationResponse.builder()
                .valid(true)
                .emailHint(maskEmail(seller.getEmail()))
                .message("Reset link is valid.")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        PasswordValidator.validate(request.getPassword());

        String normalized = normalizeToken(request.getToken());
        Seller seller = sellerRepository
                .findByPasswordResetToken(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This reset link is invalid or has expired. Please request a new one."));

        if (isTokenExpired(seller)) {
            clearResetToken(seller);
            sellerRepository.save(seller);
            throw new ResourceNotFoundException(
                    "This reset link has expired. Please request a new password reset email.");
        }

        seller.setPassword(passwordEncoder.encode(request.getPassword()));
        clearResetToken(seller);
        sellerRepository.save(seller);

        return new MessageResponse("Your password has been updated successfully. You can now log in.");
    }

    private void assertCanResetPassword(Seller seller) {
        // Email verification is not required — the reset link sent to their inbox proves ownership.
        SellerAccountStatus status = seller.getStatus();
        if (status == SellerAccountStatus.suspended) {
            throw new ForbiddenException("Your account is suspended. Please contact support.");
        }
        if (status == SellerAccountStatus.inactive) {
            throw new ForbiddenException("Your account is inactive. Please contact support.");
        }
    }

    private boolean isTokenExpired(Seller seller) {
        return seller.getPasswordResetExpiresAt() == null
                || seller.getPasswordResetExpiresAt().isBefore(LocalDateTime.now());
    }

    private void clearResetToken(Seller seller) {
        seller.setPasswordResetToken(null);
        seller.setPasswordResetExpiresAt(null);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String trimmed = token.trim();
        try {
            return URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }

    private String buildDisplayName(Seller seller) {
        String first = seller.getFirstName() != null ? seller.getFirstName().trim() : "";
        String last = seller.getLastName() != null ? seller.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        if (!full.isBlank()) {
            return full;
        }
        if (seller.getBusinessName() != null && !seller.getBusinessName().isBlank()) {
            return seller.getBusinessName().trim();
        }
        return "Seller";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal =
                local.length() <= 2 ? local.charAt(0) + "***" : local.substring(0, 2) + "***";
        return maskedLocal + "@" + parts[1];
    }
}
