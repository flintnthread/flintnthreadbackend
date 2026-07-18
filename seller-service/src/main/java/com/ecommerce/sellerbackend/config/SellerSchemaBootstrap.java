package com.ecommerce.sellerbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures optional seller-app tables exist when ddl-auto=none.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SellerSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTables() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_preferences (
                    seller_id INT NOT NULL PRIMARY KEY,
                    push_notifications TINYINT(1) NOT NULL DEFAULT 1,
                    order_updates TINYINT(1) NOT NULL DEFAULT 1,
                    payout_alerts TINYINT(1) NOT NULL DEFAULT 1,
                    vacation_mode TINYINT(1) NOT NULL DEFAULT 0,
                    dark_mode TINYINT(1) NOT NULL DEFAULT 0,
                    language VARCHAR(10) NOT NULL DEFAULT 'en-IN',
                    biometric_login TINYINT(1) NOT NULL DEFAULT 0,
                    updated_at DATETIME NULL,
                    CONSTRAINT fk_seller_preferences_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            log.info("seller_preferences table is ready");
        } catch (Exception ex) {
            log.warn("Could not ensure seller_preferences table: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_live_chat_messages (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    seller_id INT NOT NULL,
                    sender_type VARCHAR(20) NOT NULL,
                    message TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_seller_live_chat_seller (seller_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            log.info("seller_live_chat_messages table is ready");
        } catch (Exception ex) {
            log.warn("Could not ensure seller_live_chat_messages table: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_support_feedbacks (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    seller_id BIGINT NOT NULL,
                    rating INT NOT NULL,
                    feedback_text TEXT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_seller_support_feedbacks_seller (seller_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            log.info("seller_support_feedbacks table is ready");
        } catch (Exception ex) {
            log.warn("Could not ensure seller_support_feedbacks table: {}", ex.getMessage());
        }

        ensureSellerSupportTables();
        ensureProductVariantMinQuantityColumn();
        ensureSellerPayoutRequestsTable();
        // Do not zero commission on startup — rates come from admin_settings and product approval.
        syncLegacyProductSkusFromVariants();
    }

    private void ensureSellerSupportTables() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_support_tickets (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    ticket_number VARCHAR(64) NOT NULL,
                    seller_id BIGINT NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    category VARCHAR(64) NOT NULL,
                    priority VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'open',
                    assigned_to BIGINT NULL,
                    last_response_by VARCHAR(32) NULL,
                    last_response_at DATETIME NULL,
                    closed_at DATETIME NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_seller_support_ticket_number (ticket_number),
                    INDEX idx_seller_support_tickets_seller (seller_id),
                    INDEX idx_seller_support_tickets_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_support_messages (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    ticket_id BIGINT NOT NULL,
                    sender_type VARCHAR(20) NOT NULL,
                    sender_id BIGINT NULL,
                    message TEXT NOT NULL,
                    attachment VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_seller_support_messages_ticket (ticket_id, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            log.info("seller_support_tickets / seller_support_messages tables are ready");
        } catch (Exception ex) {
            log.warn("Could not ensure seller support ticket tables: {}", ex.getMessage());
        }
    }

    private void ensureSellerPayoutRequestsTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seller_payout_requests (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    seller_id BIGINT NOT NULL,
                    order_id BIGINT NOT NULL,
                    requested_amount DECIMAL(12,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    seller_note VARCHAR(500) NULL,
                    admin_note VARCHAR(500) NULL,
                    transaction_ref VARCHAR(100) NULL,
                    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    reviewed_at DATETIME NULL,
                    paid_at DATETIME NULL,
                    reviewed_by_admin_id BIGINT NULL,
                    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    INDEX idx_seller_id (seller_id),
                    INDEX idx_order_id (order_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            log.info("seller_payout_requests table is ready");
        } catch (Exception ex) {
            log.warn("Could not ensure seller_payout_requests table: {}", ex.getMessage());
        }
    }

    private void ensureProductVariantMinQuantityColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'product_variants'
                      AND COLUMN_NAME = 'min_quantity'
                    """,
                    Integer.class);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE product_variants ADD COLUMN min_quantity INT NULL AFTER stock");
            log.info("product_variants.min_quantity column is ready");
        } catch (Exception ex) {
            log.warn("Could not ensure product_variants.min_quantity column: {}", ex.getMessage());
        }
    }

    private void syncLegacyProductSkusFromVariants() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE products p
                    INNER JOIN (
                        SELECT product_id, MIN(id) AS variant_id
                        FROM product_variants
                        GROUP BY product_id
                    ) first_variant ON first_variant.product_id = p.id
                    INNER JOIN product_variants v ON v.id = first_variant.variant_id
                    SET p.sku = v.sku
                    WHERE p.sku LIKE 'SKU-%'
                      AND v.sku IS NOT NULL
                      AND v.sku <> ''
                    """);
            if (updated > 0) {
                log.info("Synced {} legacy product SKUs from first variant", updated);
            }
        } catch (Exception ex) {
            log.warn("Could not sync legacy product SKUs: {}", ex.getMessage());
        }
    }
}
