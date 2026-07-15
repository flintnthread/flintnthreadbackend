package com.ecommerce.adminbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds seller email-verification links from admin (resend).
 * Uses the public seller API host so links work on any device.
 */
@Component
public class SellerEmailVerificationUrlHelper {

    @Value("${app.seller.backend.public-url:${APP_BACKEND_PUBLIC_URL:https://flintnthread.online}}")
    private String sellerBackendPublicUrl;

    @Value("${app.seller.frontend.base-url:${APP_FRONTEND_BASE_URL:https://flintnthread.online/Seller}}")
    private String sellerFrontendBaseUrl;

    @Value("${app.seller.frontend.email-verify-redirect-url:}")
    private String sellerEmailVerifyRedirectUrl;

    /**
     * Prefer seller-service API verify endpoint (same as seller registration emails).
     */
    public String buildEmailLinkClickUrl(String emailVerificationToken) {
        return trimTrailingSlash(sellerBackendPublicUrl)
                + "/api/auth/verify-email?token="
                + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    public String buildFrontendTokenUrl(String emailVerificationToken) {
        String base = trimTrailingSlash(resolveVerifyPageBaseUrl());
        return base + "?token=" + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    private String resolveVerifyPageBaseUrl() {
        if (sellerEmailVerifyRedirectUrl != null && !sellerEmailVerifyRedirectUrl.isBlank()) {
            return sellerEmailVerifyRedirectUrl.trim();
        }
        return trimTrailingSlash(sellerFrontendBaseUrl) + "/verify-email";
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
