package com.ecommerce.sellerbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class EmailVerificationUrlHelper {

    @Value("${app.backend.public-url:http://localhost:8083}")
    private String backendPublicUrl;

    @Value("${app.frontend.base-url:http://localhost:8081}")
    private String frontendBaseUrl;

    @Value("${app.frontend.email-verify-redirect-url:}")
    private String emailVerifyRedirectUrl;

    /**
     * Link placed in the signup verification email. Opens the seller app, which confirms the
     * token via the API and shows the OTP entry page.
     */
    public String buildEmailLinkClickUrl(String emailVerificationToken) {
        return buildFrontendTokenUrl(emailVerificationToken);
    }

    /**
     * Direct frontend link with token (mobile app / SPA can call POST /confirm-email-link on load).
     */
    public String buildFrontendTokenUrl(String emailVerificationToken) {
        String base = trimTrailingSlash(resolveOtpPageBaseUrl());
        return base + "?token=" + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
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

    private String resolveOtpPageBaseUrl() {
        if (emailVerifyRedirectUrl != null && !emailVerifyRedirectUrl.isBlank()) {
            return emailVerifyRedirectUrl.trim();
        }
        return trimTrailingSlash(frontendBaseUrl) + "/verify-email";
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
