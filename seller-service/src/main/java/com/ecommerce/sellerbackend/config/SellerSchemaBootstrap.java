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
    }
}
