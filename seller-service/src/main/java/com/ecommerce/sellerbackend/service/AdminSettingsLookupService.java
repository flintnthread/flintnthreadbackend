package com.ecommerce.sellerbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminSettingsLookupService {

    private static final BigDecimal DEFAULT_COMMISSION_B2C = new BigDecimal("15.00");

    private final PlatformIntegrationSettings platformIntegrationSettings;

    public BigDecimal getSellerCommissionPercent() {
        return platformIntegrationSettings.readSetting(PlatformIntegrationSettings.KEY_COMMISSION_B2C)
                .flatMap(AdminSettingsLookupService::parsePositiveDecimal)
                .orElse(DEFAULT_COMMISSION_B2C);
    }

    private static java.util.Optional<BigDecimal> parsePositiveDecimal(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            if (value.compareTo(BigDecimal.ZERO) > 0) {
                return java.util.Optional.of(value);
            }
        } catch (NumberFormatException ignored) {
            // fall through to default
        }
        return java.util.Optional.empty();
    }
}
