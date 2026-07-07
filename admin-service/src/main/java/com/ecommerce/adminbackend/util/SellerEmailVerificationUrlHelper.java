package com.ecommerce.adminbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SellerEmailVerificationUrlHelper {

    @Value("${app.seller.frontend.base-url:http://localhost:8081}")
    private String sellerFrontendBaseUrl;

    @Value("${app.seller.frontend.email-verify-redirect-url:}")
    private String sellerEmailVerifyRedirectUrl;

    public String buildEmailLinkClickUrl(String emailVerificationToken) {
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
