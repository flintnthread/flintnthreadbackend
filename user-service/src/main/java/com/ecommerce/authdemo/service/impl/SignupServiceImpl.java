package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.SignupCompleteDTO;
import com.ecommerce.authdemo.dto.SignupSendEmailOtpDTO;
import com.ecommerce.authdemo.dto.SignupSendPhoneOtpDTO;
import com.ecommerce.authdemo.dto.SignupVerifyEmailOtpDTO;
import com.ecommerce.authdemo.entity.Otp;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.EmailSendException;
import com.ecommerce.authdemo.exception.InvalidMobileException;
import com.ecommerce.authdemo.exception.OtpExpiredException;
import com.ecommerce.authdemo.exception.OtpNotFoundException;
import com.ecommerce.authdemo.exception.SignupException;
import com.ecommerce.authdemo.exception.SmsSendException;
import com.ecommerce.authdemo.exception.TooManyAttemptsException;
import com.ecommerce.authdemo.exception.TooManyRequestsException;
import com.ecommerce.authdemo.repository.OtpRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.security.JwtUtil;
import com.ecommerce.authdemo.service.EmailService;
import com.ecommerce.authdemo.service.ReferralService;
import com.ecommerce.authdemo.service.SignupService;
import com.ecommerce.authdemo.service.SmsService;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.OtpUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dedicated Amazon-style registration: verify email via email OTP, then phone via SMS OTP,
 * with a display username collected up front.
 */
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupServiceImpl.class);

    private static final String OTP_AUTH_PASSWORD_PLACEHOLDER = "OTP_AUTH_NOT_SET";
    private static final String MOBILE_REGEX = "^[6-9]\\d{9}$";
    private static final String EMAIL_OTP_PREFIX = "signup-email:";
    private static final String PHONE_OTP_PREFIX = "signup-phone:";

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final SmsService smsService;
    private final EmailService emailService;
    private final ReferralService referralService;
    private final WalletService walletService;

    @Override
    @Transactional
    public OtpResponseDTO sendEmailOtp(SignupSendEmailOtpDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (email == null) {
            throw new SignupException("Email is required", "EMAIL_REQUIRED");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new SignupException(
                    "An account with this email already exists. Please sign in.",
                    "EMAIL_EXISTS");
        }

        String identifier = EMAIL_OTP_PREFIX + email;
        String otp = createAndReturnOtp(identifier);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailSendException("Unable to send OTP to email");
        }

        return OtpResponseDTO.builder()
                .success(true)
                .message("OTP sent successfully via EMAIL")
                .deliveryChannel("EMAIL")
                .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                .email(email)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> verifyEmailOtp(SignupVerifyEmailOtpDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (email == null) {
            throw new SignupException("Email is required", "EMAIL_REQUIRED");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new SignupException(
                    "An account with this email already exists. Please sign in.",
                    "EMAIL_EXISTS");
        }

        assertOtpValid(EMAIL_OTP_PREFIX + email, dto.getOtp());
        otpRepository.deleteByIdentifier(EMAIL_OTP_PREFIX + email);
        otpRepository.flush();

        String emailVerifiedToken = jwtUtil.generateSignupEmailToken(email);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Email verified successfully");
        result.put("email", email);
        result.put("emailVerifiedToken", emailVerifiedToken);
        result.put("nextStep", "ADD_PHONE");
        return result;
    }

    @Override
    @Transactional
    public OtpResponseDTO sendPhoneOtp(SignupSendPhoneOtpDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        String mobile = normalizeMobile(dto.getMobile());

        if (email == null) {
            throw new SignupException("Email is required", "EMAIL_REQUIRED");
        }
        if (mobile == null || !mobile.matches(MOBILE_REGEX)) {
            throw new InvalidMobileException("Invalid mobile number");
        }
        if (!jwtUtil.isValidSignupEmailToken(dto.getEmailVerifiedToken(), email)) {
            throw new SignupException(
                    "Email verification expired. Please verify your email again.",
                    "EMAIL_TOKEN_INVALID");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new SignupException(
                    "An account with this email already exists. Please sign in.",
                    "EMAIL_EXISTS");
        }
        if (userRepository.findByContactNumber(mobile).isPresent()) {
            throw new SignupException(
                    "This mobile number is already linked to another account. Please sign in or use a different number.",
                    "PHONE_EXISTS");
        }

        String identifier = PHONE_OTP_PREFIX + mobile;
        String otp = createAndReturnOtp(identifier);

        try {
            smsService.sendSms(
                    "+91" + mobile,
                    "Dear Flint & Thread customer, " + otp
                            + " is your OTP, valid for the next 1 minutes to verify your mobile number for Flint & Thread account registration. Please DO NOT disclose it to anyone. Flint & Thread (India) Private Limited will never ask for this OTP on WhatsApp, phone call, or email."
            );
        } catch (SmsSendException e) {
            throw e;
        } catch (Exception e) {
            throw new SmsSendException(
                    "Unable to send OTP SMS. Check Twilio in Admin → Platform Settings.");
        }

        return OtpResponseDTO.builder()
                .success(true)
                .message("OTP sent successfully via SMS")
                .deliveryChannel("SMS")
                .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                .email(email)
                .maskedPhone(maskPhone(mobile))
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO completeSignup(SignupCompleteDTO dto) {
        String username = normalizeUsername(dto.getUsername());
        String email = normalizeEmail(dto.getEmail());
        String mobile = normalizeMobile(dto.getMobile());

        if (username == null) {
            throw new SignupException("Username is required", "USERNAME_REQUIRED");
        }
        if (email == null) {
            throw new SignupException("Email is required", "EMAIL_REQUIRED");
        }
        if (mobile == null || !mobile.matches(MOBILE_REGEX)) {
            throw new InvalidMobileException("Invalid mobile number");
        }
        if (!jwtUtil.isValidSignupEmailToken(dto.getEmailVerifiedToken(), email)) {
            throw new SignupException(
                    "Email verification expired. Please verify your email again.",
                    "EMAIL_TOKEN_INVALID");
        }

        assertOtpValid(PHONE_OTP_PREFIX + mobile, dto.getOtp());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new SignupException(
                    "An account with this email already exists. Please sign in.",
                    "EMAIL_EXISTS");
        }
        if (userRepository.findByContactNumber(mobile).isPresent()) {
            throw new SignupException(
                    "This mobile number is already linked to another account.",
                    "PHONE_EXISTS");
        }
        if (userRepository.existsByUsername(username)
                || userRepository.findByUsername(username).isPresent()) {
            throw new SignupException(
                    "This username is already taken. Please choose another.",
                    "USERNAME_EXISTS");
        }

        otpRepository.deleteByIdentifier(PHONE_OTP_PREFIX + mobile);
        otpRepository.deleteByIdentifier(EMAIL_OTP_PREFIX + email);
        otpRepository.flush();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setContactNumber(mobile);
        user.setPassword(OTP_AUTH_PASSWORD_PLACEHOLDER);
        user.setVerified(true);
        user.setRole(Role.USER);

        if (dto.getReferralCode() != null && !dto.getReferralCode().trim().isEmpty()) {
            String refInput = dto.getReferralCode().trim().toUpperCase();
            Optional<User> referrerOpt = referralService.findReferrerByReferralCode(refInput);
            if (referrerOpt.isPresent() && !referrerOpt.get().getId().equals(0L)) {
                user.setReferredBy(referrerOpt.get().getId());
            }
        }

        User saved = userRepository.saveAndFlush(user);
        log.info("Signup complete userId={} email={} mobile={}", saved.getId(), email, mobile);

        try {
            referralService.generateCodes(saved.getId(), saved.getUsername());
        } catch (Exception e) {
            log.error("Referral code generation failed for signup user id={}: {}",
                    saved.getId(), e.getMessage(), e);
        }

        try {
            walletService.getOrCreateWallet(Math.toIntExact(saved.getId()));
        } catch (Exception e) {
            log.warn("FNT wallet ensure failed for signup user id={}: {}", saved.getId(), e.getMessage());
        }

        try {
            emailService.sendWelcomeEmail(email, username, username,
                    saved.getReferralCode() != null ? saved.getReferralCode() : "");
        } catch (Exception e) {
            log.warn("Welcome email failed for {}: {}", email, e.getMessage());
        }

        String token = jwtUtil.generateToken(email, Role.USER.name(), saved.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .role(Role.USER.name())
                .userId(saved.getId())
                .email(email)
                .contactNumber(mobile)
                .displayName(username)
                .build();
    }

    private String createAndReturnOtp(String identifier) {
        enforceResendCooldown(identifier);
        String otp = otpUtil.generateOtp();
        otpRepository.deleteByIdentifier(identifier);

        Otp entity = new Otp();
        entity.setIdentifier(identifier);
        entity.setOtp(otp);
        entity.setAttempts(0);
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.saveAndFlush(entity);
        return otp;
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

        String entered = String.valueOf(rawOtp).trim();
        String saved = String.valueOf(otpEntity.getOtp()).trim();
        if (!saved.equals(entered)) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            otpRepository.saveAndFlush(otpEntity);
            throw new SignupException("Invalid OTP", "INVALID_OTP");
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        return email.trim().toLowerCase();
    }

    private static String normalizeMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) return null;
        return mobile.trim();
    }

    private static String normalizeUsername(String username) {
        if (username == null) return null;
        String trimmed = username.trim().replaceAll("\\s+", " ");
        if (trimmed.length() < 2) return null;
        return trimmed;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "******";
        return "******" + phone.substring(phone.length() - 4);
    }
}
