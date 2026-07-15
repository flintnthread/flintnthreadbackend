package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.dto.LoginRequestDTO;
import com.ecommerce.authdemo.dto.VerifyOtpDTO;
import com.ecommerce.authdemo.entity.AdminUser;
import com.ecommerce.authdemo.entity.Otp;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.EmailSendException;
import com.ecommerce.authdemo.exception.InvalidIdentifierException;
import com.ecommerce.authdemo.exception.InvalidMobileException;
import com.ecommerce.authdemo.exception.OtpExpiredException;
import com.ecommerce.authdemo.exception.OtpNotFoundException;
import com.ecommerce.authdemo.exception.SmsSendException;
import com.ecommerce.authdemo.exception.TooManyAttemptsException;
import com.ecommerce.authdemo.exception.TooManyRequestsException;
import com.ecommerce.authdemo.repository.AdminUserRepository;
import com.ecommerce.authdemo.repository.OtpRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.security.JwtUtil;
import com.ecommerce.authdemo.service.AuthService;
import com.ecommerce.authdemo.service.EmailService;
import com.ecommerce.authdemo.service.ReferralService;
import com.ecommerce.authdemo.service.SmsService;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.OtpUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    /** OTP-only accounts do not use password login; satisfies NOT NULL DB constraint. */
    private static final String OTP_AUTH_PASSWORD_PLACEHOLDER = "OTP_AUTH_NOT_SET";

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final AdminUserRepository adminUserRepository;
    private final OtpRepository otpRepository;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final SmsService smsService;
    private final EmailService emailService;
    private final ReferralService referralService;
    private final WalletService walletService;

    /* ======================================================
       SEND OTP
    ====================================================== */
    @Override
    @Transactional
    public String sendOtp(LoginRequestDTO dto) {

        String identifier;

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            identifier = dto.getEmail().trim().toLowerCase();

        } else if (dto.getMobile() != null &&
                !dto.getMobile().trim().isEmpty()) {

            if (!dto.getMobile().matches("^[6-9]\\d{9}$")) {
                throw new InvalidMobileException("Invalid mobile number");
            }

            identifier = dto.getMobile().trim();

        } else {
            throw new InvalidIdentifierException(
                    "Email or Mobile is required");
        }

        Optional<Otp> oldOtp =
                otpRepository.findTopByIdentifierOrderByExpiryTimeDesc(
                        identifier);

        if (oldOtp.isPresent() &&
                oldOtp.get()
                        .getExpiryTime()
                        .minusMinutes(4)
                        .isAfter(LocalDateTime.now())) {

            throw new TooManyRequestsException(
                    "Please wait before requesting another OTP");
        }

        String otp = otpUtil.generateOtp();

        otpRepository.deleteByIdentifier(identifier);

        Otp entity = new Otp();
        entity.setIdentifier(identifier);
        entity.setOtp(otp);
        entity.setAttempts(0);
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        otpRepository.saveAndFlush(entity);

        if (identifier.contains("@")) {

            try {
                emailService.sendOtpEmail(identifier, otp);
            } catch (EmailSendException e) {
                throw e;
            } catch (Exception e) {
                throw new EmailSendException("Unable to send OTP to email");
            }

        } else {
            try {
                smsService.sendSms(
                        "+91" + identifier,
                        "Dear Flint & Thread customer, " + otp
                                + " is your OTP, valid for the next 1 minutes to verify your mobile number for Flint & Thread customer login and account security. Please DO NOT disclose it to anyone. Flint & Thread (India) Private Limited will never ask for this OTP on WhatsApp, phone call, or email."
                );
            } catch (SmsSendException e) {
                throw e;
            } catch (Exception e) {
                throw new SmsSendException(
                        "Unable to send OTP SMS. Check Twilio in Admin → Platform Settings.");
            }
        }

        return identifier.contains("@") ? "EMAIL" : "SMS";
    }

    /* ======================================================
       VERIFY OTP
    ====================================================== */

    @Override
    @Transactional
    public AuthResponseDTO verifyOtp(VerifyOtpDTO dto) {

        log.info("========== VERIFY OTP START ==========");

        String identifier;

        if (dto.getEmail() != null &&
                !dto.getEmail().trim().isEmpty()) {

            identifier = dto.getEmail()
                    .trim()
                    .toLowerCase();

        } else if (dto.getMobile() != null &&
                !dto.getMobile().trim().isEmpty()) {

            identifier = dto.getMobile().trim();

        } else {
            throw new RuntimeException(
                    "Email or Mobile required");
        }

        log.info("Identifier: {}", identifier);

        Otp otpEntity =
                otpRepository
                        .findTopByIdentifierOrderByExpiryTimeDesc(
                                identifier)
                        .orElseThrow(() ->
                                new OtpNotFoundException(
                                        "OTP not found"));

        log.info("OTP row found");

        if (otpEntity.getAttempts() >= 5) {
            throw new TooManyAttemptsException(
                    "Too many attempts. Try again later.");
        }

        if (otpEntity.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new OtpExpiredException(
                    "OTP expired");
        }

        String enteredOtp =
                String.valueOf(dto.getOtp()).trim();

        String savedOtp =
                String.valueOf(otpEntity.getOtp()).trim();

        log.info("Saved OTP: {}", savedOtp);
        log.info("Entered OTP: {}", enteredOtp);

        if (!savedOtp.equals(enteredOtp)) {

            otpEntity.setAttempts(
                    otpEntity.getAttempts() + 1
            );

            otpRepository.saveAndFlush(otpEntity);

            throw new RuntimeException(
                    "Invalid OTP");
        }

        log.info("OTP matched");

        Role role = Role.USER;
        User existingUser = null;

        if (adminUserRepository
                .findByEmail(identifier)
                .isPresent()) {

            role = Role.ADMIN;

        } else if (
                sellerRepository
                        .findByEmail(identifier)
                        .isPresent()

                        ||

                        sellerRepository
                                .findByMobileNumber(identifier)
                                .isPresent()
        ) {

            role = Role.SELLER;

        } else {

            Optional<User> user =
                    userRepository
                            .findByEmail(identifier)
                            .or(() ->
                                    userRepository
                                            .findByContactNumber(identifier)
                            );

            if (user.isEmpty()) {

                log.info("Creating new user");

                User newUser = new User();
                applyOtpUserIdentity(newUser, identifier);
                newUser.setVerified(true);
                newUser.setRole(Role.USER);

                // Handle referral code if provided
                User referrer = null;
                if (dto.getReferralCode() != null && !dto.getReferralCode().trim().isEmpty()) {
                    String refInput = dto.getReferralCode().trim().toUpperCase();
                    log.info("NEW USER REFERRAL: Looking up referral code: {}", refInput);
                    
                    Optional<User> referrerOpt = referralService.findReferrerByReferralCode(refInput);
                    log.info("NEW USER REFERRAL: findByReferralCode result present: {}", referrerOpt.isPresent());
                    
                    if (referrerOpt.isPresent()) {
                        referrer = referrerOpt.get();
                        newUser.setReferredBy(referrer.getId());
                        log.info("NEW USER REFERRAL: Set referredBy={} for new user", referrer.getId());
                    } else {
                        log.warn("NEW USER REFERRAL: No user found with referral code: {}", refInput);
                    }
                } else {
                    log.info("NEW USER REFERRAL: No referral code provided or empty");
                }

                User savedUser = userRepository.saveAndFlush(newUser);
                log.info("New user saved with id={}", savedUser.getId());

                try {
                    referralService.generateCodes(
                            savedUser.getId(),
                            savedUser.getUsername()
                    );
                } catch (Exception e) {
                    log.error(
                            "Referral code generation failed for new user id={}: {}",
                            savedUser.getId(),
                            e.getMessage(),
                            e
                    );
                }

                if (referrer != null) {
                    log.info(
                            "NEW USER REFERRAL: linked referrer {} — count applies when friend completes first paid order",
                            referrer.getId()
                    );
                }

                existingUser = savedUser;
                log.info("existingUser set to savedUser with id={}", existingUser.getId());

            } else {
                // Existing user found - check if referral code needs to be processed
                existingUser = user.get();
                log.info("Existing user found with id={}", existingUser.getId());
                
                // Process referral code if provided (for users without existing referral)
                if (dto.getReferralCode() != null && !dto.getReferralCode().trim().isEmpty()) {
                    try {
                        processExistingUserReferralLogin(existingUser, dto.getReferralCode());
                    } catch (Exception e) {
                        log.error(
                                "Referral login processing failed for user id={}: {}",
                                existingUser.getId(),
                                e.getMessage(),
                                e
                        );
                    }
                } else {
                    log.info("EXISTING USER REFERRAL: No referral code provided");
                }
            }
        }

        log.info("Deleting old OTP");

        otpRepository.deleteByIdentifier(identifier);
        otpRepository.flush();

        // Generate JWT based on role
        String token;
        Long userId;

        if (role == Role.ADMIN) {
            AdminUser admin = adminUserRepository.findByEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            token = jwtUtil.generateToken(identifier, role.name(), admin.getId());
            userId = admin.getId();
            log.info("Generated JWT for ADMIN id={}", userId);
            
        } else if (role == Role.SELLER) {
            Seller seller = sellerRepository.findByEmail(identifier)
                    .or(() -> sellerRepository.findByMobileNumber(identifier))
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            token = jwtUtil.generateToken(identifier, role.name(), seller.getId());
            userId = seller.getId();
            log.info("Generated JWT for SELLER id={}", userId);
            
        } else {
            // USER role - use the existingUser that was already processed
            User finalUser = existingUser;
            
            if (finalUser == null) {
                log.info("existingUser is null, fetching from repository for identifier: {}", identifier);
                finalUser = userRepository.findByEmail(identifier)
                        .or(() -> userRepository.findByContactNumber(identifier))
                        .orElse(null);
            }
            
            if (finalUser == null) {
                log.error("User not found for identifier: {}", identifier);
                throw new RuntimeException("User not found after OTP verification. Please try again.");
            }
            
            token = jwtUtil.generateToken(identifier, role.name(), finalUser.getId());
            userId = finalUser.getId();
            log.info("Generated JWT for USER id={}", userId);

            try {
                walletService.getOrCreateWallet(Math.toIntExact(userId));
                log.info("FNT Wallet ensured for user id={}", userId);
            } catch (Exception e) {
                log.warn("FNT wallet ensure failed for user id={}: {}", userId, e.getMessage());
            }
        }

        log.info("========== VERIFY OTP SUCCESS ==========");

        return new AuthResponseDTO(
                token,
                role.name(),
                userId
        );
    }

    private void applyOtpUserIdentity(User user, String identifier) {
        if (identifier.contains("@")) {
            user.setEmail(identifier);
            user.setUsername(identifier);
        } else {
            user.setContactNumber(identifier);
            user.setUsername(identifier);
            user.setEmail(identifier + "@mobile.flintnthread.in");
        }
        user.setPassword(OTP_AUTH_PASSWORD_PLACEHOLDER);
    }

    private void processExistingUserReferralLogin(User existingUser, String referralCode) {
        log.info("EXISTING USER REFERRAL: Referral code provided: {}", referralCode);
        log.info("EXISTING USER REFERRAL: Current referredBy: {}", existingUser.getReferredBy());

        if (existingUser.getReferredBy() != null) {
            log.info(
                    "EXISTING USER REFERRAL: User already has referredBy={}, skipping",
                    existingUser.getReferredBy()
            );
            return;
        }

        String refInput = referralCode.trim().toUpperCase();
        Optional<User> referrerOpt = referralService.findReferrerByReferralCode(refInput);
        if (referrerOpt.isEmpty()) {
            log.warn("EXISTING USER REFERRAL: No user found with referral code: {}", refInput);
            return;
        }

        User referrer = referrerOpt.get();
        if (referrer.getId().equals(existingUser.getId())) {
            log.warn("EXISTING USER REFERRAL: self-referral skipped for userId={}", existingUser.getId());
            return;
        }
        existingUser.setReferredBy(referrer.getId());
        userRepository.saveAndFlush(existingUser);
        log.info(
                "EXISTING USER REFERRAL: linked referrer {} — count applies when user completes first paid order",
                referrer.getId()
        );
    }
}
