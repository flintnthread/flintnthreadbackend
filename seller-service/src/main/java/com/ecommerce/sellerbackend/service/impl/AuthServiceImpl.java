package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.LoginRequest;
import com.ecommerce.sellerbackend.dto.LoginResponse;
import com.ecommerce.sellerbackend.dto.RefreshTokenResponse;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.ForbiddenException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.exception.UnauthorizedException;
import com.ecommerce.sellerbackend.repository.SellerRegistrationPaymentRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.AuthService;
import com.ecommerce.sellerbackend.service.JwtService;
import com.ecommerce.sellerbackend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final DateTimeFormatter LOGIN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.US);

    private final SellerRepository sellerRepository;
    private final SellerRegistrationPaymentRepository registrationPaymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        String identifier = request.getIdentifier().trim();
        Seller seller = findSeller(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(accountNotFoundMessage(identifier)));

        if (seller.getPassword() == null || seller.getPassword().isBlank()) {
            throw new UnauthorizedException("Invalid email/mobile or password.");
        }

        if (!matchesPassword(request.getPassword(), seller.getPassword())) {
            throw new UnauthorizedException("Invalid email/mobile or password.");
        }

        assertCanLogin(seller);

        LocalDateTime loginAt = LocalDateTime.now();
        seller.setLastLoginAt(loginAt);
        String resolvedIp = null;
        if (clientIp != null && !clientIp.isBlank()) {
            resolvedIp = clientIp.trim();
            if (resolvedIp.length() > 45) {
                resolvedIp = resolvedIp.substring(0, 45);
            }
            seller.setLastLoginIp(resolvedIp);
        }
        sellerRepository.save(seller);

        sendLoginSecurityAlertAfterCommit(seller, loginAt, userAgent, resolvedIp);

        String accessToken = jwtService.generateAccessToken(seller.getId(), seller.getEmail());
        boolean subscriptionActive = resolveSubscriptionActive(seller);
        boolean paymentPending = resolvePaymentPending(seller, subscriptionActive);
        String subscriptionExpiresAt = resolveSubscriptionExpiresAt(seller.getId());

        return LoginResponse.builder()
                .sellerId(seller.getId())
                .email(seller.getEmail())
                .mobile(seller.getMobile())
                .firstName(seller.getFirstName())
                .lastName(seller.getLastName())
                .businessName(seller.getBusinessName())
                .emailVerified(Boolean.TRUE.equals(seller.getEmailVerified()))
                .profileCompleted(Boolean.TRUE.equals(seller.getProfileCompleted()))
                .status(seller.getStatus() != null
                        ? seller.getStatus().name()
                        : SellerAccountStatus.pending.name())
                .subscriptionActive(subscriptionActive)
                .paymentPending(paymentPending)
                .subscriptionExpiresAt(subscriptionExpiresAt)
                .accessToken(accessToken)
                .expiresIn(jwtService.getExpirySeconds())
                .build();
    }

    private boolean resolveSubscriptionActive(Seller seller) {
        if (!Boolean.TRUE.equals(seller.getProfileCompleted())) {
            return true;
        }
        registrationPaymentRepository.ensureTable();
        return registrationPaymentRepository.isSubscriptionActive(seller.getId());
    }

    private boolean resolvePaymentPending(Seller seller, boolean subscriptionActive) {
        if (!Boolean.TRUE.equals(seller.getProfileCompleted())) {
            return false;
        }
        registrationPaymentRepository.ensureTable();
        return registrationPaymentRepository.hasEverPaid(seller.getId()) && !subscriptionActive;
    }

    private String resolveSubscriptionExpiresAt(Long sellerId) {
        registrationPaymentRepository.ensureTable();
        SellerRegistrationPaymentRepository.PaymentRecord record =
                registrationPaymentRepository.findBySellerId(sellerId);
        if (record == null || record.getSubscriptionExpiresAt() == null) {
            return null;
        }
        return record.getSubscriptionExpiresAt().toString();
    }

    @Override
    public RefreshTokenResponse refreshSession(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required.");
        }
        String token = bearerToken.substring(7).trim();
        String newToken = jwtService.refreshAccessToken(token);
        return RefreshTokenResponse.builder()
                .accessToken(newToken)
                .expiresIn(jwtService.getExpirySeconds())
                .build();
    }

    private void sendLoginSecurityAlertAfterCommit(
            Seller seller,
            LocalDateTime loginAt,
            String userAgent,
            String clientIp) {
        final String mailTo = seller.getEmail();
        if (mailTo == null || mailTo.isBlank()) {
            return;
        }
        final String displayName = buildDisplayName(seller);
        final String loginTime = LOGIN_TIME_FORMAT.format(loginAt);
        final String device = describeDevice(userAgent);
        final String ipAddress = clientIp != null ? clientIp : "Unknown";

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mailService.sendLoginSecurityAlertEmail(
                            mailTo,
                            displayName,
                            loginTime,
                            device,
                            "Unknown Location",
                            ipAddress
                    );
                } catch (Exception ex) {
                    log.error("Failed to send login security alert to {}", mailTo, ex);
                }
            }
        });
    }

    private String buildDisplayName(Seller seller) {
        String first = seller.getFirstName() != null ? seller.getFirstName().trim() : "";
        String last = seller.getLastName() != null ? seller.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Seller" : full;
    }

    private String describeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);

        String browser = "Unknown Browser";
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browser = "Edge";
        } else if (ua.contains("chrome/") || ua.contains("crios/")) {
            browser = "Chrome";
        } else if (ua.contains("firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome/") && !ua.contains("crios/")) {
            browser = "Safari";
        }

        String os = "Unknown OS";
        if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            os = "iOS";
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            os = "macOS";
        }

        boolean mobile = ua.contains("mobile")
                || ua.contains("android")
                || ua.contains("iphone")
                || ua.contains("ipad");
        String formFactor = mobile ? "Mobile" : "Desktop";
        return formFactor + " (" + browser + " on " + os + ")";
    }

    private String accountNotFoundMessage(String identifier) {
        if (identifier.contains("@")) {
            return "No seller account found with this email. Please register first or check the email you used during signup.";
        }
        return "No seller account found with this mobile number. Please register first or check the number you used during signup.";
    }

    private Optional<Seller> findSeller(String identifier) {
        if (identifier.contains("@")) {
            return sellerRepository.findByEmailIgnoreCase(identifier.toLowerCase(Locale.ROOT));
        }
        String digits = identifier.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return Optional.empty();
        }
        String lastTen = digits.substring(digits.length() - 10);
        return sellerRepository.findByMobileDigits(lastTen);
    }

    private boolean matchesPassword(String rawPassword, String storedHash) {
        String hash = storedHash.trim();
        if (hash.startsWith("$2y$")) {
            hash = "$2a$" + hash.substring(4);
        }
        return passwordEncoder.matches(rawPassword, hash);
    }

    private void assertCanLogin(Seller seller) {
        if (!Boolean.TRUE.equals(seller.getEmailVerified())) {
            throw new ForbiddenException(
                    "Please verify your email before logging in. Check your inbox for the verification link.");
        }

        SellerAccountStatus status = seller.getStatus();
        if (status == null) {
            throw new ForbiddenException("Your account is not active. Please contact support.");
        }

        switch (status) {
            case active -> {
                return;
            }
            case pending -> {
                if (Boolean.TRUE.equals(seller.getProfileCompleted())) {
                    return;
                }
                throw new ForbiddenException(
                        "Your seller account is pending approval. Please complete your profile or wait for admin review.");
            }
            case rejected -> {
                return;
            }
            case email_pending -> throw new ForbiddenException(
                    "Email verification is pending. Please verify your email to continue.");
            case inactive -> throw new ForbiddenException(
                    "Your account is inactive. Please contact support to reactivate.");
            case suspended -> throw new ForbiddenException(
                    "Your account has been suspended. Please contact support.");
            default -> throw new ForbiddenException("Your account is not allowed to log in.");
        }
    }
}
