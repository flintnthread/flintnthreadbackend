package com.ecommerce.authdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds optional {@code orders} columns when ddl-auto=none (production MySQL).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureOrderColumns() {
        String[] statements = {
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS wallet_deduction DECIMAL(10, 2) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS referral_inviter_discount_applied BOOLEAN DEFAULT FALSE",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address2 VARCHAR(500) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_awb_code VARCHAR(100) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_courier_name VARCHAR(150) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_tracking_url VARCHAR(500) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_status VARCHAR(100) NULL",
                "ALTER TABLE orders MODIFY COLUMN shiprocket_status VARCHAR(500) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_label_url VARCHAR(1000) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_invoice_url VARCHAR(1000) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS shiprocket_manifest_url VARCHAR(1000) NULL",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(10, 2) NULL",
        };

        for (String sql : statements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ex) {
                log.warn("Order schema bootstrap skipped ({}): {}", sql, ex.getMessage());
            }
        }
        log.info("Order schema bootstrap completed");
    }
}
