package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.OtpSentResponse;
import com.ecommerce.sellerbackend.dto.OtpVerifiedResponse;
import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.dto.SendOtpRequest;
import com.ecommerce.sellerbackend.dto.VerifyOtpRequest;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.DuplicateResourceException;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.service.SellerRegistrationService;
import com.ecommerce.sellerbackend.service.SellerUniqueIdService;
import com.ecommerce.sellerbackend.service.SmsService;
import com.ecommerce.sellerbackend.service.otp.OtpVerificationStore;
import com.ecommerce.sellerbackend.util.EmailVerificationUrlHelper;
import com.ecommerce.sellerbackend.util.MobileUtils;
import com.ecommerce.sellerbackend.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerRegistrationServiceImpl implements SellerRegistrationService {

    private final SellerRepository sellerRepository;
    private final OtpVerificationStore otpStore;
    private final SmsService smsService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationUrlHelper emailVerificationUrlHelper;
    private final SellerUniqueIdService sellerUniqueIdService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:2}")
    private int otpExpiryMinutes;

    @Value("${app.otp.verification-token-minutes:30}")
    private int verificationTokenMinutes;

    @Value("${app.otp.dev-mode:false}")
    private boolean otpDevMode;

    @Override
    public OtpSentResponse sendOtp(SendOtpRequest request) {
        String mobileDigits = MobileUtils.normalizeDigits(request.getMobile());
        if (mobileDigits.length() != 10) {
            throw new IllegalArgumentException("Enter a valid 10-digit mobile number.");
        }

        if (sellerRepository.countByMobileDigits(mobileDigits) > 0) {
            throw new DuplicateResourceException("This mobile number is already registered.");
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        otpStore.saveOtp(mobileDigits, otp, expiresAt);

        String mobileE164 = MobileUtils.toE164India(request.getMobile());
        smsService.sendOtp(mobileE164, otp);

        String formattedMobile = "+91 " + mobileDigits.substring(0, 5) + " " + mobileDigits.substring(5);
        String message = otpDevMode
                ? "OTP generated for " + formattedMobile + " (dev mode)."
                : "OTP sent via SMS to " + formattedMobile + ".";
        return new OtpSentResponse(
                message,
                MobileUtils.maskMobile(mobileDigits),
                otpDevMode ? otp : null
        );
    }

    @Override
    public OtpVerifiedResponse verifyOtp(VerifyOtpRequest request) {
        String mobileDigits = MobileUtils.normalizeDigits(request.getMobile());
        var challengeOpt = otpStore.findByMobile(mobileDigits);
        if (challengeOpt.isEmpty()) {
            throw new IllegalArgumentException("No OTP found for this number. Please request a new OTP.");
        }

        var challenge = challengeOpt.get();
        if (challenge.otpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }
        if (!challenge.otp().equals(request.getOtp().trim())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        String token = otpStore.markVerified(
                mobileDigits,
                LocalDateTime.now().plusMinutes(verificationTokenMinutes)
        );
        if (token == null) {
            throw new IllegalStateException("Unable to verify OTP. Please try again.");
        }

        return new OtpVerifiedResponse(
                true,
                token,
                "Mobile number verified successfully."
        );
    }

    @Override
    @Transactional
    public RegisterSellerResponse register(RegisterSellerRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        PasswordValidator.validate(request.getPassword());

        String mobileDigits = MobileUtils.normalizeDigits(request.getMobile());
        String token = request.getMobileVerificationToken().trim();
        Optional<String> verifiedMobile = otpStore.peekVerificationToken(token);
        if (verifiedMobile.isEmpty() || !verifiedMobile.get().equals(mobileDigits)) {
            throw new IllegalArgumentException("Mobile verification expired or invalid. Please verify OTP again.");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (sellerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        if (sellerRepository.countByMobileDigits(mobileDigits) > 0) {
            throw new DuplicateResourceException("An account with this mobile number already exists.");
        }

        String emailVerificationToken = generateEmailVerificationToken();
        LocalDateTime now = LocalDateTime.now();

        Seller seller = new Seller();
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName() != null ? request.getLastName().trim() : "";
        seller.setFirstName(firstName);
        seller.setLastName(lastName);
        seller.setFullName((firstName + " " + lastName).trim());
        seller.setEmail(email);
        seller.setMobile(mobileDigits);
        seller.setPassword(passwordEncoder.encode(request.getPassword()));
        seller.setEmailVerified(false);
        seller.setEmailVerificationToken(emailVerificationToken);
        seller.setOtp(null);
        seller.setOtpExpiresAt(null);
        seller.setMobileVerified(true);
        seller.setMobileVerifiedAt(now);
        seller.setOtpSentAt(null);
        seller.setStatus(SellerAccountStatus.email_pending);
        seller.setProfileCompleted(false);

        seller = sellerRepository.save(seller);
        sellerUniqueIdService.ensureSellerUniqueId(seller);

        final String mailTo = seller.getEmail();
        final String displayName = buildDisplayName(seller);
        final String verifyLink = emailVerificationUrlHelper.buildEmailLinkClickUrl(emailVerificationToken);
        final String mobileVerificationToken = token;

        mailService.sendEmailVerificationLinkEmail(mailTo, displayName, verifyLink);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                otpStore.consumeVerificationToken(mobileVerificationToken);
            }
        });

        return new RegisterSellerResponse(
                seller.getId(),
                "Registration successful! Please check your email and click the verification link to activate your account. "
                        + "The verification link will expire in 1 hour.",
                true
        );
    }

    private String generateOtp() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }

    private String generateEmailVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildDisplayName(Seller seller) {
        String first = seller.getFirstName() != null ? seller.getFirstName().trim() : "";
        String last = seller.getLastName() != null ? seller.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Seller" : full;
    }
}
