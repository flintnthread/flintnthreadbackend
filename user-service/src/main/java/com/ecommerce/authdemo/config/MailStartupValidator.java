package com.ecommerce.authdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class MailStartupValidator {

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.from:}")
    private String from;

    @PostConstruct
    void validateMailConfig() {
        if (!StringUtils.hasText(password) || password.startsWith("${")) {
            log.error(
                    "[MAIL] SendGrid API key missing. Set SENDGRID_API_KEY (or spring.mail.password in "
                            + "application-local.properties). OTP emails will fail until configured."
            );
            return;
        }
        log.info("[MAIL] SendGrid configured host={} from={}", host, from);
    }
}
