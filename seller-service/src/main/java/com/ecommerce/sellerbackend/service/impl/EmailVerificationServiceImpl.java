package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.EmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.MessageResponse;
import com.ecommerce.sellerbackend.dto.ResendEmailOtpRequest;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationRequest;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.VerifyEmailOtpRequest;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.EmailVerificationService;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.util.EmailVerificationUrlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final SellerRepository sellerRepository;
    private final MailService mailService;
    private final EmailVerificationUrlHelper emailVerificationUrlHelper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.email-verification-otp-minutes:10}")
    private int emailOtpExpiryMinutes;

    @Value("${app.auth.email-verification-expiry-hours:1}")
    private int emailVerificationExpiryHours;

    @Override
    @Transactional
    public StartEmailVerificationResponse confirmEmailLink(StartEmailVerificationRequest request) {
        String token = request.getToken().trim();
        Seller seller = sellerRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification link."));

        String email = seller.getEmail().toLowerCase(Locale.ROOT);

        if (Boolean.TRUE.equals(seller.getEmailVerified())) {
            return new StartEmailVerificationResponse(
                    "Your email is already verified. You can log in now.",
                    email,
                    false,
                    true
            );
        }

        assertVerificationLinkNotExpired(seller);

        String otp = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        seller.setOtp(otp);
        seller.setOtpExpiresAt(now.plusMinutes(emailOtpExpiryMinutes));
        seller.setOtpSentAt(now);
        sellerRepository.save(seller);

        mailService.sendEmailVerificationOtpEmail(
                seller.getEmail(),
                buildDisplayName(seller),
                otp);

        return new StartEmailVerificationResponse(
                "OTP has been sent to your email address. Please enter the 6-digit code below.",
                email,
                true,
                false
        );
    }

    @Override
    @Transactional
    public EmailVerificationResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String otp = request.getOtp().trim();

        Seller seller = sellerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No pending registration found for this email."));

        if (Boolean.TRUE.equals(seller.getEmailVerified())) {
            return new EmailVerificationResponse(
                    "Your email is already verified. You can log in now.",
                    true,
                    email,
                    seller.getId()
            );
        }

        if (seller.getOtp() == null || seller.getOtpExpiresAt() == null) {
            throw new IllegalArgumentException(
                    "No verification code yet. Open the link in your signup email first.");
        }

        if (seller.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Click the email link again or resend.");
        }

        if (!seller.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid verification code. Please try again.");
        }

        seller.setEmailVerified(true);
        seller.setEmailVerifiedAt(LocalDateTime.now());
        seller.setEmailVerificationToken(null);
        seller.setOtp(null);
        seller.setOtpExpiresAt(null);
        seller.setStatus(SellerAccountStatus.active);
        sellerRepository.save(seller);

        return new EmailVerificationResponse(
                "Email verified successfully. You can now log in to your seller account.",
                true,
                email,
                seller.getId()
        );
    }

    @Override
    @Transactional
    public MessageResponse resendEmailOtp(ResendEmailOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Seller seller = sellerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email."));

        if (Boolean.TRUE.equals(seller.getEmailVerified())) {
            return new MessageResponse("Your email is already verified. You can log in now.");
        }

        assertVerificationLinkNotExpired(seller);

        final String mailTo = seller.getEmail();
        final String displayName = buildDisplayName(seller);

        if (seller.getOtp() == null || seller.getOtpExpiresAt() == null) {
            String linkToken = seller.getEmailVerificationToken();
            if (linkToken == null || linkToken.isBlank()) {
                throw new IllegalStateException("Verification link is no longer valid. Please contact support.");
            }
            String verifyLink = emailVerificationUrlHelper.buildEmailLinkClickUrl(linkToken);
            mailService.sendEmailVerificationLinkEmail(mailTo, displayName, verifyLink);
            return new MessageResponse("Verification link resent. Click the link in your email to receive a code.");
        }

        String otp = generateOtp();
        seller.setOtp(otp);
        seller.setOtpExpiresAt(LocalDateTime.now().plusMinutes(emailOtpExpiryMinutes));
        seller.setOtpSentAt(LocalDateTime.now());
        sellerRepository.save(seller);

        mailService.sendEmailVerificationOtpEmail(mailTo, displayName, otp);

        return new MessageResponse("A new verification code has been sent to your email.");
    }

    private void assertVerificationLinkNotExpired(Seller seller) {
        LocalDateTime base = seller.getOtpSentAt() != null ? seller.getOtpSentAt() : seller.getCreatedAt();
        if (base == null) {
            return;
        }
        LocalDateTime expiresAt = base.plusHours(emailVerificationExpiryHours);
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Verification link has expired. Please sign up again or contact support.");
        }
    }

    private String generateOtp() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }

    private String buildDisplayName(Seller seller) {
        String first = seller.getFirstName() != null ? seller.getFirstName().trim() : "";
        String last = seller.getLastName() != null ? seller.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Seller" : full;
    }

}
