package com.ecommerce.authdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShiprocketInternalKeyStartupLogger {

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    @PostConstruct
    public void logConfig() {
        boolean keyOk = internalServiceKey != null && !internalServiceKey.isBlank();
        log.info("Shiprocket internal API: internalKeyConfigured={}", keyOk);
        if (!keyOk) {
            log.error(
                    "INTERNAL_SERVICE_KEY / app.internal-service-key is BLANK — "
                            + "admin/seller Shiprocket push will get HTTP 403. "
                            + "Set INTERNAL_SERVICE_KEY in /etc/flintnthread/application.properties"
            );
        }
    }
}
