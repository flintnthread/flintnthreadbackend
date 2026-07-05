package com.ecommerce.adminbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SellerEmailVerificationUrlHelper {

    @Value("${app.seller.backend.public-url:http://localhost:8083}")
    private String sellerBackendPublicUrl;

    public String buildEmailLinkClickUrl(String emailVerificationToken) {
        String base = trimTrailingSlash(sellerBackendPublicUrl);
        return base + "/api/auth/verify-email?token="
                + URLEncoder.encode(emailVerificationToken, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
