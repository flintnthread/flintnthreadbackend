package com.ecommerce.sellerbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingsLookupService {

    private static final BigDecimal DEFAULT_COMMISSION_B2C = new BigDecimal("15.00");

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal getSellerCommissionPercent() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM admin_settings WHERE setting_key = 'commission_b2c' LIMIT 1",
                    String.class);
            if (value != null && !value.isBlank()) {
                return new BigDecimal(value.trim());
            }
        } catch (Exception ex) {
            log.debug("commission_b2c not loaded from admin_settings: {}", ex.getMessage());
        }
        return DEFAULT_COMMISSION_B2C;
    }
}
