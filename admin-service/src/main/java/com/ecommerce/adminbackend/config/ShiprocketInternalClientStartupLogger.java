package com.ecommerce.adminbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShiprocketInternalClientStartupLogger {

    @Value("${app.user-service-url:http://127.0.0.1:8080}")
    private String userServiceUrl;

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    @PostConstruct
    public void logConfig() {
        boolean keyOk = internalServiceKey != null && !internalServiceKey.isBlank();
        log.info(
                "Shiprocket internal client: userServiceUrl={} internalKeyConfigured={}",
                userServiceUrl,
                keyOk
        );
        if (!keyOk) {
            log.error(
                    "INTERNAL_SERVICE_KEY / app.internal-service-key is BLANK — "
                            + "Admin Push to Shiprocket will fail. Set the same key on user-service and admin-service."
            );
        }
    }
}
