package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerCategory;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminSettingsLookupService {

    public static final String KEY_COMMISSION_B2C = PlatformIntegrationSettings.KEY_COMMISSION_B2C;
    public static final String KEY_COMMISSION_B2B = "commission_b2b";

    private static final BigDecimal DEFAULT_COMMISSION_B2C = new BigDecimal("15.00");
    private static final BigDecimal DEFAULT_COMMISSION_B2B = new BigDecimal("7.00");

    private final PlatformIntegrationSettings platformIntegrationSettings;
    private final SellerRepository sellerRepository;

    /** B2C default — kept for callers without seller context. */
    public BigDecimal getSellerCommissionPercent() {
        return getCommissionPercent(SellerCategory.b2c);
    }

    public BigDecimal getSellerCommissionPercent(Long sellerId) {
        if (sellerId == null) {
            return getCommissionPercent(SellerCategory.b2c);
        }
        SellerCategory category = sellerRepository.findById(sellerId)
                .map(Seller::getSellerCategory)
                .orElse(SellerCategory.b2c);
        return getCommissionPercent(category);
    }

    public BigDecimal getCommissionPercent(SellerCategory category) {
        if (category == SellerCategory.b2b) {
            return platformIntegrationSettings.readSetting(KEY_COMMISSION_B2B)
                    .flatMap(AdminSettingsLookupService::parseNonNegativeDecimal)
                    .orElse(DEFAULT_COMMISSION_B2B);
        }
        return platformIntegrationSettings.readSetting(KEY_COMMISSION_B2C)
                .flatMap(AdminSettingsLookupService::parseNonNegativeDecimal)
                .orElse(DEFAULT_COMMISSION_B2C);
    }

    private static java.util.Optional<BigDecimal> parseNonNegativeDecimal(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            if (value.compareTo(BigDecimal.ZERO) >= 0) {
                return java.util.Optional.of(value);
            }
        } catch (NumberFormatException ignored) {
            // fall through to default
        }
        return java.util.Optional.empty();
    }
}
