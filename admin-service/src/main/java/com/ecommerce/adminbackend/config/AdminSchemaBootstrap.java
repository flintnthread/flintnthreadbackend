package com.ecommerce.adminbackend.config;

import com.ecommerce.adminbackend.logging.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LogFactory.getLogger(AdminSchemaBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureSellerPayoutRequestsTable();
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
}
