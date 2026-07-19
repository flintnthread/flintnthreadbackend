package com.ecommerce.sellerbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds public links for seller email verification.
 * Production defaults use {@code https://seller.flintnthread.in} for the seller app
 * and {@code https://flintnthread.in} for API links (matches live TLS certificate SANs).
 */
@Component
public class EmailVerificationUrlHelper {

    @Value("${app.backend.public-url:https://flintnthread.in}")
    private String backendPublicUrl;

    @Value("${app.frontend.base-url:https://seller.flintnthread.in}")
    private String frontendBaseUrl;

    @Value("${app.frontend.email-verify-redirect-url:}")
    private String emailVerifyRedirectUrl;

    /**
     * Link in the signup email. Hits seller-service ({@code /api/auth/verify-email})
     * so verification works on any phone/PC without needing Expo local server.
     */
    public String buildEmailLinkClickUrl(String emailVerificationToken) {
        return trimTrailingSlash(backendPublicUrl())
                + "/api/auth/verify-email?token="
                + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    /**
     * Direct frontend link with token (mobile app / SPA can call POST /confirm-email-link on load).
     */
    public String buildFrontendTokenUrl(String emailVerificationToken) {
        String base = trimTrailingSlash(resolveOtpPageBaseUrl());
        return base + "?token=" + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    public String buildLoginPageUrl(String email) {
        String base = trimTrailingSlash(frontendBaseUrl()) + "/login";
        if (email == null || email.isBlank()) {
            return base + "?verified=1";
        }
        return base + "?verified=1&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    public String buildOtpPageRedirect(String email, boolean otpSent, boolean alreadyVerified) {
        String base = trimTrailingSlash(resolveOtpPageBaseUrl());
        StringBuilder url = new StringBuilder(base).append("?email=")
                .append(URLEncoder.encode(email, StandardCharsets.UTF_8));
        if (otpSent) {
            url.append("&otpSent=1");
        }
        if (alreadyVerified) {
            url.append("&verified=1");
        }
        return url.toString();
    }

    public String buildOtpPageRedirectError(String message) {
        String base = trimTrailingSlash(resolveOtpPageBaseUrl());
        return base + "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    public String getBackendPublicUrl() {
        return trimTrailingSlash(backendPublicUrl());
    }

    public String getFrontendBaseUrl() {
        return trimTrailingSlash(frontendBaseUrl());
    }

    private String resolveOtpPageBaseUrl() {
        if (emailVerifyRedirectUrl != null && !emailVerifyRedirectUrl.isBlank()) {
            return SslSafePublicUrl.normalize(emailVerifyRedirectUrl.trim());
        }
        return trimTrailingSlash(frontendBaseUrl()) + "/verify-email";
    }

    private String backendPublicUrl() {
        return SslSafePublicUrl.normalize(backendPublicUrl);
    }

    private String frontendBaseUrl() {
        return SslSafePublicUrl.normalize(frontendBaseUrl);
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
