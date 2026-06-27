package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.service.WalletSchemaEnsurer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletSchemaEnsurerImpl implements WalletSchemaEnsurer {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void ensureWalletTables() {
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
                        type VARCHAR(16) NOT NULL,
                        description TEXT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_uwt_user_id (user_id),
                        INDEX idx_uwt_order_id (order_id)
                    )
                    """);
        } catch (Exception error) {
            log.error("[WALLET] ensureWalletTables failed: {}", error.getMessage(), error);
            throw error;
        }
    }
}
