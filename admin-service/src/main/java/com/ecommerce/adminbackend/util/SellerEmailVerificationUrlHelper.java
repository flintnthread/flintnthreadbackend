package com.ecommerce.adminbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds seller email-verification links from admin (resend).
 * Uses {@code https://flintnthread.in} so links match the live TLS certificate SANs.
 */
@Component
public class SellerEmailVerificationUrlHelper {

    @Value("${app.seller.backend.public-url:${APP_BACKEND_PUBLIC_URL:https://flintnthread.in}}")
    private String sellerBackendPublicUrl;

    @Value("${app.seller.frontend.base-url:${APP_FRONTEND_BASE_URL:https://flintnthread.in/Seller}}")
    private String sellerFrontendBaseUrl;

    @Value("${app.seller.frontend.email-verify-redirect-url:}")
    private String sellerEmailVerifyRedirectUrl;

    /**
     * Prefer seller-service API verify endpoint (same as seller registration emails).
     */
    public String buildEmailLinkClickUrl(String emailVerificationToken) {
        return trimTrailingSlash(normalize(sellerBackendPublicUrl))
                + "/api/auth/verify-email?token="
                + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    public String buildFrontendTokenUrl(String emailVerificationToken) {
        String base = trimTrailingSlash(resolveVerifyPageBaseUrl());
        return base + "?token=" + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    private String resolveVerifyPageBaseUrl() {
        if (sellerEmailVerifyRedirectUrl != null && !sellerEmailVerifyRedirectUrl.isBlank()) {
            return normalize(sellerEmailVerifyRedirectUrl.trim());
        }
        return trimTrailingSlash(normalize(sellerFrontendBaseUrl)) + "/verify-email";
    }

    /**
     * Live cert SANs are {@code *.flintnthread.in} only — rewrite {@code .online} to avoid
     * {@code NET::ERR_CERT_COMMON_NAME_INVALID}.
     */
    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String u = url.trim();
        u = replaceHost(u, "https://www.flintnthread.online", "https://www.flintnthread.in");
        u = replaceHost(u, "https://seller.flintnthread.online", "https://seller.flintnthread.in");
        u = replaceHost(u, "https://admin.flintnthread.online", "https://admin.flintnthread.in");
        u = replaceHost(u, "https://flintnthread.online", "https://flintnthread.in");
        u = replaceHost(u, "http://www.flintnthread.online", "https://www.flintnthread.in");
        u = replaceHost(u, "http://seller.flintnthread.online", "https://seller.flintnthread.in");
        u = replaceHost(u, "http://admin.flintnthread.online", "https://admin.flintnthread.in");
        u = replaceHost(u, "http://flintnthread.online", "https://flintnthread.in");
        return u;
    }

    private static String replaceHost(String url, String from, String to) {
        if (url.regionMatches(true, 0, from, 0, from.length())) {
            return to + url.substring(from.length());
        }
        return url.replace(from, to);
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
