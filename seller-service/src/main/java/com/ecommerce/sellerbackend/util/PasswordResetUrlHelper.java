package com.ecommerce.sellerbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PasswordResetUrlHelper {

    @Value("${app.backend.public-url:https://flintnthread.in}")
    private String backendPublicUrl;

    @Value("${app.frontend.base-url:https://seller.flintnthread.in}")
    private String frontendBaseUrl;

    @Value("${app.frontend.password-reset-redirect-url:}")
    private String passwordResetRedirectUrl;

    /**
     * Link placed in the password reset email. Hits seller-service
     * ({@code /api/auth/reset-password}) like email verification, so the hop
     * works on any phone/PC without needing Expo local server.
     */
    public String buildEmailLinkClickUrl(String resetToken) {
        return trimTrailingSlash(SslSafePublicUrl.normalize(backendPublicUrl))
                + "/api/auth/reset-password?token="
                + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
    }

    public String buildResetPageRedirect(String token) {
        String base = trimTrailingSlash(resolveResetPageBaseUrl());
        return base + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    public String buildResetPageRedirectError(String message) {
        String base = trimTrailingSlash(resolveResetPageBaseUrl());
        return base + "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String resolveResetPageBaseUrl() {
        if (passwordResetRedirectUrl != null && !passwordResetRedirectUrl.isBlank()) {
            return SslSafePublicUrl.normalize(passwordResetRedirectUrl.trim());
        }
        return trimTrailingSlash(SslSafePublicUrl.normalize(frontendBaseUrl)) + "/resetpassword";
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
