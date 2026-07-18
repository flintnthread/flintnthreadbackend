package com.ecommerce.sellerbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PasswordResetUrlHelper {

    @Value("${app.backend.public-url:https://flintnthread.in}")
    private String backendPublicUrl;

    @Value("${app.frontend.base-url:https://flintnthread.in/Seller}")
    private String frontendBaseUrl;

    @Value("${app.frontend.password-reset-redirect-url:}")
    private String passwordResetRedirectUrl;

    /**
     * Link placed in the password reset email. Opens the seller app reset-password page.
     */
    public String buildEmailLinkClickUrl(String resetToken) {
        return buildResetPageRedirect(resetToken);
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
