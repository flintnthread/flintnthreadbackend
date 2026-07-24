package com.ecommerce.adminbackend.config;

import com.ecommerce.adminbackend.logging.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures admin-owned support tables exist. Does not alter the shared {@code orders} table.
 */
@Component
@RequiredArgsConstructor
public class AdminSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LogFactory.getLogger(AdminSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureSellerPayoutRequestsTable();
        ensureSellerSupportTables();
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
}
