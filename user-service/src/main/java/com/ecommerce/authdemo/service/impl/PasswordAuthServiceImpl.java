package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.dto.ForgotPasswordResetDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordSendOtpDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordVerifyOtpDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.PasswordLoginDTO;
import com.ecommerce.authdemo.entity.Otp;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.AuthException;
import com.ecommerce.authdemo.exception.EmailSendException;
import com.ecommerce.authdemo.exception.OtpExpiredException;
import com.ecommerce.authdemo.exception.OtpNotFoundException;
import com.ecommerce.authdemo.exception.SmsSendException;
import com.ecommerce.authdemo.exception.TooManyAttemptsException;
import com.ecommerce.authdemo.exception.TooManyRequestsException;
import com.ecommerce.authdemo.repository.OtpRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.security.JwtUtil;
import com.ecommerce.authdemo.service.EmailService;
import com.ecommerce.authdemo.service.PasswordAuthService;
import com.ecommerce.authdemo.service.SmsService;
import com.ecommerce.authdemo.service.WalletService;
import com.ecommerce.authdemo.util.OtpUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PasswordAuthServiceImpl implements PasswordAuthService {

    private static final Logger log = LoggerFactory.getLogger(PasswordAuthServiceImpl.class);

    private static final String OTP_AUTH_PLACEHOLDER = "OTP_AUTH_NOT_SET";
    private static final String MOBILE_REGEX = "^[6-9]\\d{9}$";
    private static final String RESET_OTP_PREFIX = "reset:";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final SmsService smsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    /** In-memory failed login attempts / lockouts keyed by normalized identifier. */
    private final ConcurrentHashMap<String, LoginAttemptState> loginAttempts = new ConcurrentHashMap<>();

    private static final class LoginAttemptState {
        int failures;
        LocalDateTime lockedUntil;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(PasswordLoginDTO dto) {
        String identifier = normalizeIdentifier(dto.getIdentifier());
        String password = dto.getPassword() == null ? "" : dto.getPassword();

        if (identifier == null) {
            throw new AuthException(
                    "No account was found with this email address or mobile number. Please create a new account to continue.",
                    "ACCOUNT_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value());
        }

        assertNotLocked(identifier);

        User user = findUserByIdentifier(identifier).orElseThrow(() ->
                new AuthException(
                        "No account was found with this email address or mobile number. Please create a new account to continue.",
                        "ACCOUNT_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));

        String stored = user.getPassword();
        if (stored == null
                || stored.isBlank()
                || OTP_AUTH_PLACEHOLDER.equals(stored)
                || !passwordEncoder.matches(password, stored)) {
            // Also try plain match for legacy non-hashed passwords (sellers-style), then re-hash later on reset.
            boolean legacyMatch = stored != null
                    && !OTP_AUTH_PLACEHOLDER.equals(stored)
                    && !stored.startsWith("$2a$")
                    && !stored.startsWith("$2b$")
                    && stored.equals(password);
            if (!legacyMatch) {
                recordFailedLogin(identifier);
                throw new AuthException(
                        "Incorrect password. Please try again.",
                        "INCORRECT_PASSWORD",
                        HttpStatus.UNAUTHORIZED.value());
            }
        }

        clearFailedLogin(identifier);

        try {
            walletService.getOrCreateWallet(Math.toIntExact(user.getId()));
        } catch (Exception e) {
            log.warn("Wallet ensure failed on login for user {}: {}", user.getId(), e.getMessage());
        }

        String subject = publicEmail(user.getEmail()) != null
                ? publicEmail(user.getEmail())
                : (user.getContactNumber() != null ? user.getContactNumber() : identifier);

        String token = jwtUtil.generateToken(subject, Role.USER.name(), user.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .role(Role.USER.name())
                .userId(user.getId())
                .email(publicEmail(user.getEmail()))
                .contactNumber(user.getContactNumber())
                .displayName(resolveDisplayName(user))
                .build();
    }

    private void assertNotLocked(String identifier) {
        LoginAttemptState state = loginAttempts.get(identifier);
        if (state == null || state.lockedUntil == null) {
            return;
        }
        if (state.lockedUntil.isAfter(LocalDateTime.now())) {
            throw new AuthException(
                    "Too many failed login attempts. Please try again in a few minutes.",
                    "ACCOUNT_LOCKED",
                    HttpStatus.TOO_MANY_REQUESTS.value());
        }
        // Lock expired — reset
        loginAttempts.remove(identifier);
    }

    private void recordFailedLogin(String identifier) {
        LoginAttemptState state = loginAttempts.computeIfAbsent(identifier, k -> new LoginAttemptState());
        state.failures += 1;
        if (state.failures >= MAX_FAILED_ATTEMPTS) {
            state.lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            state.failures = 0;
        }
    }

    private void clearFailedLogin(String identifier) {
        loginAttempts.remove(identifier);
    }

    @Override
    @Transactional
    public OtpResponseDTO sendForgotPasswordOtp(ForgotPasswordSendOtpDTO dto) {
        String identifier = normalizeIdentifier(dto.getIdentifier());
        if (identifier == null) {
            throw new AuthException(
                    "No account was found with this email address or mobile number. Please create a new account to continue.",
                    "ACCOUNT_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value());
        }

        User user = findUserByIdentifier(identifier).orElseThrow(() ->
                new AuthException(
                        "No account was found with this email address or mobile number. Please create a new account to continue.",
                        "ACCOUNT_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));

        String otpKey = RESET_OTP_PREFIX + identifier;
        String otp = createAndReturnOtp(otpKey);

        boolean isEmail = identifier.contains("@");
        if (isEmail) {
            try {
                emailService.sendOtpEmail(identifier, otp);
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
                    .email(identifier)
                    .build();
        }

        String mobile = user.getContactNumber() != null ? user.getContactNumber() : identifier;
        try {
            smsService.sendSms(
                    "+91" + mobile,
                    "Dear Flint & Thread customer, " + otp
                            + " is your OTP to reset your password. Valid for 5 minutes. Do not share this OTP. Flint & Thread (India) Private Limited."
            );
        } catch (SmsSendException e) {
            throw e;
        } catch (Exception e) {
            throw new SmsSendException("Unable to send OTP SMS.");
        }

        return OtpResponseDTO.builder()
                .success(true)
                .message("OTP sent successfully via SMS")
                .deliveryChannel("SMS")
                .nextStep(OtpResponseDTO.NEXT_VERIFY_OTP)
                .maskedPhone(maskPhone(mobile))
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> verifyForgotPasswordOtp(ForgotPasswordVerifyOtpDTO dto) {
        String identifier = normalizeIdentifier(dto.getIdentifier());
        if (identifier == null) {
            throw new AuthException(
                    "No account was found with this email address or mobile number. Please create a new account to continue.",
                    "ACCOUNT_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value());
        }

        User user = findUserByIdentifier(identifier).orElseThrow(() ->
                new AuthException(
                        "No account was found with this email address or mobile number. Please create a new account to continue.",
                        "ACCOUNT_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));

        assertOtpValid(RESET_OTP_PREFIX + identifier, dto.getOtp());
        otpRepository.deleteByIdentifier(RESET_OTP_PREFIX + identifier);
        otpRepository.flush();

        String resetToken = jwtUtil.generatePasswordResetToken(identifier, user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "OTP verified successfully");
        result.put("resetToken", resetToken);
        result.put("nextStep", "NEW_PASSWORD");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(ForgotPasswordResetDTO dto) {
        String identifier = normalizeIdentifier(dto.getIdentifier());
        String password = dto.getPassword() == null ? "" : dto.getPassword().trim();
        String confirm = dto.getConfirmPassword() == null ? "" : dto.getConfirmPassword().trim();

        if (identifier == null) {
            throw new AuthException(
                    "No account was found with this email address or mobile number. Please create a new account to continue.",
                    "ACCOUNT_NOT_FOUND",
                    HttpStatus.NOT_FOUND.value());
        }
        if (password.length() < 6) {
            throw new AuthException(
                    "Password must be at least 6 characters.",
                    "WEAK_PASSWORD",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (!password.equals(confirm)) {
            throw new AuthException(
                    "Confirm password must match password.",
                    "PASSWORD_MISMATCH",
                    HttpStatus.BAD_REQUEST.value());
        }

        Long userId = jwtUtil.validatePasswordResetToken(dto.getResetToken(), identifier);
        if (userId == null) {
            throw new AuthException(
                    "Reset session expired. Please verify OTP again.",
                    "RESET_TOKEN_INVALID",
                    HttpStatus.UNAUTHORIZED.value());
        }

        User user = userRepository.findById(userId).orElseThrow(() ->
                new AuthException(
                        "No account was found with this email address or mobile number. Please create a new account to continue.",
                        "ACCOUNT_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));

        user.setPassword(passwordEncoder.encode(password));
        userRepository.saveAndFlush(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Password updated successfully");
        return result;
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier);
        }
        return userRepository.findByContactNumber(identifier);
    }

    private String normalizeIdentifier(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String value = raw.trim();
        if (value.contains("@")) {
            return value.toLowerCase();
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        if (!digits.matches(MOBILE_REGEX)) {
            return null;
        }
        return digits;
    }

    private String createAndReturnOtp(String identifier) {
        Optional<Otp> oldOtp =
                otpRepository.findTopByIdentifierOrderByExpiryTimeDesc(identifier);
        if (oldOtp.isPresent()
                && oldOtp.get().getExpiryTime().minusMinutes(4).isAfter(LocalDateTime.now())) {
            throw new TooManyRequestsException("Please wait before requesting another OTP");
        }

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
            throw new AuthException("Invalid OTP", "INVALID_OTP", HttpStatus.BAD_REQUEST.value());
        }
    }

    private static String publicEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String lower = email.trim().toLowerCase();
        if (lower.endsWith("@mobile.flintnthread.in")
                || lower.endsWith("@mobile.flintnthread.online")
                || lower.matches("^\\d{10}@.*")) {
            return null;
        }
        return email;
    }

    private static String resolveDisplayName(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            String u = user.getUsername().trim();
            if (!u.equals(user.getContactNumber()) && !u.contains("@")) {
                return u;
            }
        }
        String email = publicEmail(user.getEmail());
        if (email != null) {
            return email.split("@")[0];
        }
        return user.getContactNumber() != null ? user.getContactNumber() : "Account";
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "******";
        return "******" + phone.substring(phone.length() - 4);
    }
}
