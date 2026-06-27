package com.ecommerce.authdemo.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures {@code user_wallet_transactions} exists even when Flyway is not configured.
 * Without this table, prepaid cancel refunds cannot be recorded in FNT Wallet.
 */
@Component
@RequiredArgsConstructor
public class UserWalletTransactionsSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserWalletTransactionsSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureUserWalletTransactionsTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_wallet (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL UNIQUE,
                        balance DECIMAL(10, 2) DEFAULT 0,
                        total_earned DECIMAL(10, 2) DEFAULT 0,
                        total_spent DECIMAL(10, 2) DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_wallet_transactions (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        order_id INT NULL,
                        amount DECIMAL(10, 2) NOT NULL,
                        type VARCHAR(10) NOT NULL,
                        description TEXT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_uwt_user_id (user_id),
                        INDEX idx_uwt_order_id (order_id)
                    )
                    """);
            log.info("[WALLET] user_wallet and user_wallet_transactions tables ready");
        } catch (Exception error) {
            log.error("[WALLET] failed to ensure wallet tables: {}",
                    error.getMessage(), error);
        }
    }
}
