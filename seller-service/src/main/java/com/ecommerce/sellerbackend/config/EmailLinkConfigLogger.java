package com.ecommerce.sellerbackend.config;

import com.ecommerce.sellerbackend.util.EmailVerificationUrlHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Logs public email-link bases at startup so misconfigured localhost links are obvious.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailLinkConfigLogger {

    private final EmailVerificationUrlHelper emailVerificationUrlHelper;

    @Value("${app.mail.allow-localhost-links:false}")
    private boolean allowLocalhostLinks;

    @Value("${spring.profiles.active:prod}")
    private String activeProfiles;

    @PostConstruct
    void logEmailLinkBases() {
        String sample = emailVerificationUrlHelper.buildEmailLinkClickUrl("startup-check-token");
        log.info(
                "Seller email verification base: backend={}, frontend={}, sampleLink={}, allowLocalhostLinks={}, profiles={}",
                emailVerificationUrlHelper.getBackendPublicUrl(),
                emailVerificationUrlHelper.getFrontendBaseUrl(),
                sample,
                allowLocalhostLinks,
                activeProfiles
        );
        String lower = sample.toLowerCase();
        if ((lower.contains("://localhost") || lower.contains("://127.0.0.1"))
                && !allowLocalhostLinks) {
            log.error(
                    "Email verification links resolve to localhost but app.mail.allow-localhost-links=false. "
                            + "Recipients on other devices cannot verify. Set APP_BACKEND_PUBLIC_URL and "
                            + "APP_FRONTEND_BASE_URL (or SPRING_PROFILES_ACTIVE=prod)."
            );
        }
        if (lower.contains("flintnthread.online")) {
            log.error(
                    "Email verification links still use flintnthread.online (TLS cert is flintnthread.in only). "
                            + "Browsers show NET::ERR_CERT_COMMON_NAME_INVALID. "
                            + "Set APP_BACKEND_PUBLIC_URL=https://flintnthread.in"
            );
        }
    }
}
