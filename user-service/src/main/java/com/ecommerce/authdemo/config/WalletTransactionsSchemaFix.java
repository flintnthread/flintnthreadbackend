package com.ecommerce.authdemo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FNT user wallets store buyer {@code users.id} in wallet_transactions.seller_id,
 * but older schemas linked that column to sellers(id). Repoint the FK on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletTransactionsSchemaFix {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixWalletTransactionsForeignKey() {
        try {
            if (!walletTransactionsTableExists()) {
                return;
            }

            fixOwnerColumnForeignKey();
            fixCreatedByForeignKey();
        } catch (Exception e) {
            log.error(
                    "[WALLET] Could not auto-fix wallet_transactions FK. "
                            + "Run src/main/resources/db/fix_wallet_transactions_fk.sql manually: {}",
                    e.getMessage()
            );
        }
    }

    private void fixOwnerColumnForeignKey() {
        String ownerColumn = resolveOwnerColumn();
        if (ownerColumn == null) {
            log.warn("[WALLET] wallet_transactions has no seller_id/user_id column; FK fix skipped");
            return;
        }

        if (referencesUsers(ownerColumn)) {
            log.info("[WALLET] wallet_transactions.{} already references users(id)", ownerColumn);
            return;
        }

        if (!referencesSellers(ownerColumn)) {
            log.info("[WALLET] wallet_transactions.{} FK does not reference sellers; fix skipped", ownerColumn);
            return;
        }

        for (String constraint : findForeignKeys(ownerColumn, "sellers")) {
            jdbcTemplate.execute(
                    "ALTER TABLE wallet_transactions DROP FOREIGN KEY `" + constraint + "`"
            );
            log.info("[WALLET] Dropped FK {} on wallet_transactions.{}", constraint, ownerColumn);
        }

        jdbcTemplate.execute(
                "ALTER TABLE wallet_transactions "
                        + "ADD CONSTRAINT fk_wallet_transactions_user "
                        + "FOREIGN KEY (`" + ownerColumn + "`) REFERENCES users(id) ON DELETE CASCADE"
        );
        log.info(
                "[WALLET] Repointed wallet_transactions.{} -> users(id)",
                ownerColumn
        );
    }

    private void fixCreatedByForeignKey() {
        if (!columnExists("created_by")) {
            return;
        }

        if (referencesUsers("created_by")) {
            log.info("[WALLET] wallet_transactions.created_by already references users(id)");
            return;
        }

        if (!referencesTable("created_by", "admin_users")) {
            return;
        }

        for (String constraint : findForeignKeys("created_by", "admin_users")) {
            jdbcTemplate.execute(
                    "ALTER TABLE wallet_transactions DROP FOREIGN KEY `" + constraint + "`"
            );
            log.info("[WALLET] Dropped FK {} on wallet_transactions.created_by", constraint);
        }

        jdbcTemplate.execute(
                "ALTER TABLE wallet_transactions "
                        + "ADD CONSTRAINT fk_wallet_transactions_created_by_user "
                        + "FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL"
        );
        log.info("[WALLET] Repointed wallet_transactions.created_by -> users(id)");
    }

    private boolean walletTransactionsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wallet_transactions'
                """,
                Integer.class
        );
        return count != null && count > 0;
    }

    private String resolveOwnerColumn() {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT COLUMN_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wallet_transactions'
                  AND COLUMN_NAME IN ('user_id', 'seller_id')
                ORDER BY FIELD(COLUMN_NAME, 'seller_id', 'user_id')
                """,
                String.class
        );
        if (columns.isEmpty()) {
            return null;
        }
        return columns.get(0);
    }

    private boolean referencesUsers(String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wallet_transactions'
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME = 'users'
                """,
                Integer.class,
                column
        );
        return count != null && count > 0;
    }

    private boolean referencesSellers(String column) {
        return referencesTable(column, "sellers");
    }

    private boolean referencesTable(String column, String referencedTable) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wallet_transactions'
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME = ?
                """,
                Integer.class,
                column,
                referencedTable
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'wallet_transactions'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                column
        );
        return count != null && count > 0;
    }

    private List<String> findForeignKeys(String column, String referencedTable) {
        return jdbcTemplate.query(
                """
                SELECT DISTINCT k.CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE k
                WHERE k.TABLE_SCHEMA = DATABASE()
                  AND k.TABLE_NAME = 'wallet_transactions'
                  AND k.COLUMN_NAME = ?
                  AND k.REFERENCED_TABLE_NAME = ?
                """,
                (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"),
                column,
                referencedTable
        );
    }
}
