package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.dto.LoginRequestDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.VerifyOtpDTO;
import com.ecommerce.authdemo.entity.AdminUser;
import com.ecommerce.authdemo.entity.Otp;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.InvalidIdentifierException;
import com.ecommerce.authdemo.exception.InvalidMobileException;
import com.ecommerce.authdemo.exception.OtpExpiredException;
import com.ecommerce.authdemo.exception.OtpNotFoundException;
import com.ecommerce.authdemo.exception.PhoneAlreadyLinkedException;
import com.ecommerce.authdemo.exception.SmsSendException;
import com.ecommerce.authdemo.exception.TooManyAttemptsException;
import com.ecommerce.authdemo.exception.TooManyRequestsException;
import com.ecommerce.authdemo.repository.AdminUserRepository;
import com.ecommerce.authdemo.repository.OtpRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.security.JwtUtil;
import com.ecommerce.authdemo.service.AuthService;
import com.ecommerce.authdemo.service.ReferralService;
import com.ecommerce.authdemo.service.SmsService;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.OtpUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Amazon-style OTP-only auth:
 * <ul>
 *   <li>Mobile → SMS OTP → open/create that Customer ID</li>
 *   <li>Email with linked phone → SMS OTP to linked phone → open that Customer ID</li>
 *   <li>Email without phone (or new email) → client adds mobile → SMS OTP → link/create</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    /** OTP-only accounts do not use password login; satisfies NOT NULL DB constraint. */
    private static final String OTP_AUTH_PASSWORD_PLACEHOLDER = "OTP_AUTH_NOT_SET";

    private static final String MOBILE_REGEX = "^[6-9]\\d{9}$";

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final AdminUserRepository adminUserRepository;
    private final OtpRepository otpRepository;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final SmsService smsService;
    private final ReferralService referralService;
    private final WalletService walletService;

    /* ======================================================
       SEND OTP
    ====================================================== */
    @Override
    @Transactional
    public OtpResponseDTO sendOtp(LoginRequestDTO dto) {

        String email = normalizeEmail(dto.getEmail());
        String mobile = normalizeMobile(dto.getMobile());

        if (email == null && mobile == null) {
            throw new InvalidIdentifierException("Email or Mobile is required");
        }

        // Email + mobile: register/link phone, then SMS OTP (keyed by email)
        if (email != null && mobile != null) {
            validateMobileOrThrow(mobile);
            assertPhoneAvailableForEmail(email, mobile);
            createAndSendSmsOtp(email, mobile);
            return OtpResponseDTO.builder()
                    .success(true)
                    .message("OTP sent successfully via SMS")
                    .deliveryChannel("SMS")
                    .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                    .email(email)
                    .maskedPhone(maskPhone(mobile))
                    .build();
        }

        // Email only: OTP to linked phone, or ask client to add a phone
        if (email != null) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent() && hasLinkedPhone(userOpt.get())) {
                String linkedPhone = userOpt.get().getContactNumber().trim();
                createAndSendSmsOtp(email, linkedPhone);
                return OtpResponseDTO.builder()
                        .success(true)
                        .message("OTP sent successfully via SMS")
                        .deliveryChannel("SMS")
                        .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                        .email(email)
                        .maskedPhone(maskPhone(linkedPhone))
                        .build();
            }

            // New email registration OR existing email without phone
            return OtpResponseDTO.builder()
                    .success(true)
                    .message("Add a mobile number to continue")
                    .nextStep(OtpResponseDTO.NEXT_ADD_PHONE)
                    .code(OtpResponseDTO.CODE_PHONE_REQUIRED)
                    .email(email)
                    .build();
        }

        // Mobile only: SMS OTP (keyed by mobile) — never ask for email
        validateMobileOrThrow(mobile);
        createAndSendSmsOtp(mobile, mobile);
        return OtpResponseDTO.builder()
                .success(true)
                .message("OTP sent successfully via SMS")
                .deliveryChannel("SMS")
                .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                .maskedPhone(maskPhone(mobile))
                .build();
    }

    /* ======================================================
       VERIFY OTP
    ====================================================== */

    @Override
    @Transactional
    public AuthResponseDTO verifyOtp(VerifyOtpDTO dto) {

        log.info("========== VERIFY OTP START ==========");

        String email = normalizeEmail(dto.getEmail());
        String mobile = normalizeMobile(dto.getMobile());

        if (email == null && mobile == null) {
            throw new RuntimeException("Email or Mobile required");
        }

        // OTP is stored under email for email-led flows, under mobile for phone-only.
        String otpIdentifier = email != null ? email : mobile;
        log.info("OTP identifier: {}", otpIdentifier);

        assertOtpValid(otpIdentifier, dto.getOtp());

        Role role = Role.USER;
        User existingUser = null;

        if (email != null && adminUserRepository.findByEmail(email).isPresent()) {
            role = Role.ADMIN;
        } else if (
                (email != null && sellerRepository.findByEmail(email).isPresent())
                        || (mobile != null && sellerRepository.findByMobileNumber(mobile).isPresent())
        ) {
            role = Role.SELLER;
        } else {
            existingUser = resolveOrCreateShopper(email, mobile, dto.getReferralCode());
        }

        log.info("Deleting old OTP for {}", otpIdentifier);
        otpRepository.deleteByIdentifier(otpIdentifier);
        otpRepository.flush();

        String token;
        Long userId;

        if (role == Role.ADMIN) {
            AdminUser admin = adminUserRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            token = jwtUtil.generateToken(email, role.name(), admin.getId());
            userId = admin.getId();
            return AuthResponseDTO.builder()
                    .token(token)
                    .role(role.name())
                    .userId(userId)
                    .email(email)
                    .displayName(admin.getEmail())
                    .build();
        }

        if (role == Role.SELLER) {
            Seller seller = sellerRepository.findByEmail(email != null ? email : "")
                    .or(() -> sellerRepository.findByMobileNumber(mobile != null ? mobile : ""))
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            String subject = email != null ? email : mobile;
            token = jwtUtil.generateToken(subject, role.name(), seller.getId());
            userId = seller.getId();
            return AuthResponseDTO.builder()
                    .token(token)
                    .role(role.name())
                    .userId(userId)
                    .email(seller.getEmail())
                    .contactNumber(seller.getMobileNumber())
                    .displayName(seller.getEmail() != null ? seller.getEmail() : seller.getMobileNumber())
                    .build();
        }

        User finalUser = existingUser;
        if (finalUser == null) {
            throw new RuntimeException("User not found after OTP verification. Please try again.");
        }

        String subject = resolveJwtSubject(finalUser, email, mobile);
        token = jwtUtil.generateToken(subject, role.name(), finalUser.getId());
        userId = finalUser.getId();
        log.info("Generated JWT for USER id={}", userId);

        try {
            walletService.getOrCreateWallet(Math.toIntExact(userId));
            log.info("FNT Wallet ensured for user id={}", userId);
        } catch (Exception e) {
            log.warn("FNT wallet ensure failed for user id={}: {}", userId, e.getMessage());
        }

        log.info("========== VERIFY OTP SUCCESS ==========");

        return AuthResponseDTO.builder()
                .token(token)
                .role(role.name())
                .userId(userId)
                .email(publicEmail(finalUser.getEmail()))
                .contactNumber(finalUser.getContactNumber())
                .displayName(resolveDisplayName(finalUser))
                .build();
    }

    /**
     * Resolve the shopper Customer ID after a successful OTP:
     * email+mobile → link/create; email only → existing linked account; mobile only → open/create.
     */
    private User resolveOrCreateShopper(String email, String mobile, String referralCode) {
        if (email != null && mobile != null) {
            return linkOrCreateEmailWithPhone(email, mobile, referralCode);
        }

        if (email != null) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException(
                            "No account found for this email. Add a mobile number to register."));
            if (!hasLinkedPhone(user)) {
                throw new RuntimeException(
                        "This email has no linked mobile number. Add a mobile number to continue.");
            }
            processExistingUserReferralLogin(user, referralCode);
            return user;
        }

        // Mobile-only: open linked account or create phone-only Customer ID
        Optional<User> byPhone = userRepository.findByContactNumber(mobile);
        if (byPhone.isPresent()) {
            User user = byPhone.get();
            processExistingUserReferralLogin(user, referralCode);
            return user;
        }

        log.info("Creating new phone-only user for {}", mobile);
        User newUser = new User();
        applyOtpUserIdentity(newUser, mobile);
        newUser.setVerified(true);
        newUser.setRole(Role.USER);
        applyReferralOnCreate(newUser, referralCode);
        User saved = userRepository.saveAndFlush(newUser);
        ensureReferralCodes(saved);
        return saved;
    }

    private User linkOrCreateEmailWithPhone(String email, String mobile, String referralCode) {
        validateMobileOrThrow(mobile);
        assertPhoneAvailableForEmail(email, mobile);

        Optional<User> byEmail = userRepository.findByEmail(email);
        Optional<User> byPhone = userRepository.findByContactNumber(mobile);

        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (byPhone.isPresent() && !byPhone.get().getId().equals(user.getId())) {
                throw new PhoneAlreadyLinkedException(
                        "This mobile number is already linked to another account");
            }
            user.setContactNumber(mobile);
            user.setVerified(true);
            processExistingUserReferralLogin(user, referralCode);
            return userRepository.saveAndFlush(user);
        }

        if (byPhone.isPresent()) {
            User phoneUser = byPhone.get();
            String existingEmail = phoneUser.getEmail();
            if (existingEmail != null
                    && !existingEmail.isBlank()
                    && !isSyntheticMobileEmail(existingEmail)
                    && !existingEmail.equalsIgnoreCase(email)) {
                throw new PhoneAlreadyLinkedException(
                        "This mobile number is already linked to another account");
            }
            // Attach real email to the existing phone account (same Customer ID)
            phoneUser.setEmail(email);
            if (phoneUser.getUsername() == null
                    || phoneUser.getUsername().isBlank()
                    || phoneUser.getUsername().equals(mobile)
                    || isSyntheticMobileEmail(phoneUser.getUsername())) {
                phoneUser.setUsername(email);
            }
            phoneUser.setVerified(true);
            processExistingUserReferralLogin(phoneUser, referralCode);
            return userRepository.saveAndFlush(phoneUser);
        }

        log.info("Creating new user with email={} mobile={}", email, mobile);
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(email);
        newUser.setContactNumber(mobile);
        newUser.setPassword(OTP_AUTH_PASSWORD_PLACEHOLDER);
        newUser.setVerified(true);
        newUser.setRole(Role.USER);
        applyReferralOnCreate(newUser, referralCode);
        User saved = userRepository.saveAndFlush(newUser);
        ensureReferralCodes(saved);
        return saved;
    }

    private void assertPhoneAvailableForEmail(String email, String mobile) {
        Optional<User> byPhone = userRepository.findByContactNumber(mobile);
        if (byPhone.isEmpty()) {
            return;
        }
        User phoneUser = byPhone.get();
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent() && byEmail.get().getId().equals(phoneUser.getId())) {
            return;
        }
        String phoneEmail = phoneUser.getEmail();
        if (byEmail.isEmpty()
                && (phoneEmail == null
                        || phoneEmail.isBlank()
                        || isSyntheticMobileEmail(phoneEmail))) {
            // Phone-only account can later attach this email on verify
            return;
        }
        if (byEmail.isEmpty()
                && phoneEmail != null
                && phoneEmail.equalsIgnoreCase(email)) {
            return;
        }
        throw new PhoneAlreadyLinkedException(
                "This mobile number is already linked to another account");
    }

    private void createAndSendSmsOtp(String otpIdentifier, String smsMobile) {
        enforceResendCooldown(otpIdentifier);

        String otp = otpUtil.generateOtp();
        otpRepository.deleteByIdentifier(otpIdentifier);

        Otp entity = new Otp();
        entity.setIdentifier(otpIdentifier);
        entity.setOtp(otp);
        entity.setAttempts(0);
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.saveAndFlush(entity);

        try {
            smsService.sendSms(
                    "+91" + smsMobile,
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

    private void enforceResendCooldown(String identifier) {
        Optional<Otp> oldOtp =
                otpRepository.findTopByIdentifierOrderByExpiryTimeDesc(identifier);

        if (oldOtp.isPresent()
                && oldOtp.get().getExpiryTime().minusMinutes(4).isAfter(LocalDateTime.now())) {
            throw new TooManyRequestsException("Please wait before requesting another OTP");
        }
    }

    private void assertOtpValid(String identifier, String rawOtp) {
        Otp otpEntity = otpRepository
                .findTopByIdentifierOrderByExpiryTimeDesc(identifier)
                .orElseThrow(() -> new OtpNotFoundException("OTP not found"));

        if (otpEntity.getAttempts() >= 5) {
            throw new TooManyAttemptsException("Too many attempts. Try again later.");
        }

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("OTP expired");
        }

        String enteredOtp = String.valueOf(rawOtp).trim();
        String savedOtp = String.valueOf(otpEntity.getOtp()).trim();

        if (!savedOtp.equals(enteredOtp)) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            otpRepository.saveAndFlush(otpEntity);
            throw new RuntimeException("Invalid OTP");
        }
    }

    private void applyOtpUserIdentity(User user, String identifier) {
        if (identifier.contains("@")) {
            user.setEmail(identifier);
            user.setUsername(identifier);
        } else {
            user.setContactNumber(identifier);
            user.setUsername(identifier);
            String existingEmail = user.getEmail();
            if (existingEmail == null || existingEmail.isBlank() || isSyntheticMobileEmail(existingEmail)) {
                user.setEmail(identifier + "@mobile.flintnthread.in");
            }
        }
        user.setPassword(OTP_AUTH_PASSWORD_PLACEHOLDER);
    }

    private void applyReferralOnCreate(User newUser, String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }
        String refInput = referralCode.trim().toUpperCase();
        Optional<User> referrerOpt = referralService.findReferrerByReferralCode(refInput);
        if (referrerOpt.isPresent()) {
            newUser.setReferredBy(referrerOpt.get().getId());
        }
    }

    private void ensureReferralCodes(User savedUser) {
        try {
            referralService.generateCodes(savedUser.getId(), savedUser.getUsername());
        } catch (Exception e) {
            log.error(
                    "Referral code generation failed for new user id={}: {}",
                    savedUser.getId(),
                    e.getMessage(),
                    e
            );
        }
    }

    private static boolean hasLinkedPhone(User user) {
        String phone = user.getContactNumber();
        return phone != null && phone.trim().matches(MOBILE_REGEX);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private static String normalizeMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        return mobile.trim();
    }

    private static void validateMobileOrThrow(String mobile) {
        if (mobile == null || !mobile.matches(MOBILE_REGEX)) {
            throw new InvalidMobileException("Invalid mobile number");
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        return "******" + phone.substring(phone.length() - 4);
    }

    private static boolean isSyntheticMobileEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        String lower = email.trim().toLowerCase();
        return lower.endsWith("@mobile.flintnthread.in")
                || lower.endsWith("@mobile.flintnthread.online")
                || lower.matches("^\\d{10}@.*");
    }

    private static String publicEmail(String email) {
        if (email == null || isSyntheticMobileEmail(email)) {
            return null;
        }
        return email;
    }

    private static String resolveDisplayName(User user) {
        String email = publicEmail(user.getEmail());
        if (email != null) {
            String local = email.split("@")[0];
            if (local != null && !local.isBlank()) {
                return local;
            }
        }
        if (user.getUsername() != null
                && !user.getUsername().isBlank()
                && !user.getUsername().equals(user.getContactNumber())
                && !isSyntheticMobileEmail(user.getUsername())) {
            return user.getUsername();
        }
        if (hasLinkedPhone(user)) {
            return user.getContactNumber();
        }
        return "Account";
    }

    private static String resolveJwtSubject(User user, String email, String mobile) {
        String publicMail = publicEmail(user.getEmail());
        if (publicMail != null) {
            return publicMail;
        }
        if (email != null) {
            return email;
        }
        if (hasLinkedPhone(user)) {
            return user.getContactNumber();
        }
        return mobile != null ? mobile : String.valueOf(user.getId());
    }

    private void processExistingUserReferralLogin(User existingUser, String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }
        if (existingUser.getReferredBy() != null) {
            return;
        }

        String refInput = referralCode.trim().toUpperCase();
        Optional<User> referrerOpt = referralService.findReferrerByReferralCode(refInput);
        if (referrerOpt.isEmpty()) {
            return;
        }

        User referrer = referrerOpt.get();
        if (referrer.getId().equals(existingUser.getId())) {
            return;
        }
        existingUser.setReferredBy(referrer.getId());
        userRepository.saveAndFlush(existingUser);
    }
}
