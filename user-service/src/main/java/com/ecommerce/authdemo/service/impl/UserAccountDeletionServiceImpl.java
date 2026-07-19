package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.UserAccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountDeletionServiceImpl implements UserAccountDeletionService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void deleteUserAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        int uid = Math.toIntExact(userId);
        log.info("Deleting all related data for user id={}", userId);

        disableReferentialIntegrity();
        try {
            deleteTicketData(userId, uid);
            deleteOrderData(userId);
            deleteUserScopedRows(userId, uid);
            userRepository.deleteById(userId);
            log.info("Deleted user id={}", userId);
        } finally {
            enableReferentialIntegrity();
        }
    }

    private void disableReferentialIntegrity() {
        if (isH2Database()) {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        } else {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        }
    }

    private void enableReferentialIntegrity() {
        if (isH2Database()) {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } else {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private boolean isH2Database() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("h2");
        } catch (SQLException ex) {
            log.warn("Could not detect database product, assuming MySQL: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Never run DELETE SQL against a missing table/column inside the account-delete
     * transaction — a failed statement can poison MySQL's transaction even when caught.
     */
    private boolean tableExists(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String[] candidates = {
                    tableName,
                    tableName.toLowerCase(Locale.ROOT),
                    tableName.toUpperCase(Locale.ROOT)
            };
            for (String candidate : candidates) {
                try (ResultSet rs = meta.getTables(catalog, null, candidate, new String[]{"TABLE", "BASE TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            // Some drivers ignore catalog; retry without it.
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE", "BASE TABLE"})) {
                return rs.next();
            }
        } catch (SQLException ex) {
            log.debug("tableExists({}) check failed: {}", tableName, ex.getMessage());
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        if (!tableExists(tableName) || columnName == null || columnName.isBlank()) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String[] tables = {
                    tableName,
                    tableName.toLowerCase(Locale.ROOT),
                    tableName.toUpperCase(Locale.ROOT)
            };
            String[] columns = {
                    columnName,
                    columnName.toLowerCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT)
            };
            for (String table : tables) {
                for (String column : columns) {
                    try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
                        if (rs.next()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (SQLException ex) {
            log.debug("columnExists({}.{}) check failed: {}", tableName, columnName, ex.getMessage());
            return false;
        }
    }

    private void deleteTicketData(Long userId, int uid) {
        safeUpdate(
                "ticket_responses_read",
                """
                DELETE FROM ticket_responses_read
                WHERE user_id = ?
                   OR response_id IN (
                       SELECT tr.id
                       FROM ticket_responses tr
                       INNER JOIN support_tickets st ON tr.ticket_id = st.id
                       WHERE st.customer_id = ?
                   )
                """,
                uid,
                uid
        );

        safeUpdate(
                "ticket_user_replies",
                """
                DELETE FROM ticket_user_replies
                WHERE user_id = ?
                   OR ticket_id IN (
                       SELECT id FROM support_tickets WHERE customer_id = ?
                   )
                """,
                uid,
                uid
        );

        safeUpdate(
                "ticket_responses",
                """
                DELETE FROM ticket_responses
                WHERE ticket_id IN (
                    SELECT id FROM support_tickets WHERE customer_id = ?
                )
                """,
                uid
        );

        safeUpdate(
                "support_tickets.order_id",
                """
                DELETE FROM support_tickets
                WHERE order_id IN (
                    SELECT id FROM orders WHERE user_id = ?
                )
                """,
                userId
        );

        safeUpdate(
                "support_tickets.customer_id",
                "DELETE FROM support_tickets WHERE customer_id = ?",
                uid
        );
    }

    private void safeUpdate(String label, String sql, Object... args) {
        // Extract first table name after FROM/UPDATE for existence probe when possible.
        String table = extractPrimaryTable(sql);
        if (table != null && !tableExists(table)) {
            log.warn("Skipping cleanup for {} — table '{}' not found", label, table);
            return;
        }
        try {
            jdbcTemplate.update(sql, args);
        } catch (Exception ex) {
            log.warn("Skipping cleanup for {} while deleting account: {}", label, ex.getMessage());
        }
    }

    private String extractPrimaryTable(String sql) {
        if (sql == null) {
            return null;
        }
        String normalized = sql.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx < 0) {
            int updateIdx = upper.indexOf("UPDATE ");
            if (updateIdx < 0) {
                return null;
            }
            String afterUpdate = normalized.substring(updateIdx + 7).trim();
            return afterUpdate.split("\\s+")[0];
        }
        String afterFrom = normalized.substring(fromIdx + 6).trim();
        return afterFrom.split("\\s+")[0];
    }

    private void deleteOrderData(Long userId) {
        if (tableExists("orders") && columnExists("orders", "address_id")) {
            safeUpdate(
                    "orders.address_id",
                    "UPDATE orders SET address_id = NULL WHERE user_id = ?",
                    userId
            );
        }

        // Optional / legacy tables — probe schema first so bad grammar never hits the TX.
        safeDeleteByOrderFk("shiprocket_webhooks", "order_id", userId);
        safeDeleteByOrderFk("shiprocket_sync_logs", "order_id", userId);
        safeDeleteByOrderFk("invoices", "order_id", userId);
        safeDeleteByOrderFk("payment_transactions", "order_id", userId);
        safeDeletePaymentsForUserOrders(userId);
        safeDeleteByOrderFk("seller_payment_invoices", "order_id", userId);
        safeDeleteByOrderFk("wallet_transactions", "order_id", userId);
        safeDeleteByOrderFk("order_status_history", "order_id", userId);

        if (tableExists("order_items") && columnExists("order_items", "order_id")) {
            safeUpdate(
                    "order_items",
                    """
                    DELETE FROM order_items
                    WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                    """,
                    userId
            );
        }

        if (tableExists("orders") && columnExists("orders", "user_id")) {
            safeUpdate("orders", "DELETE FROM orders WHERE user_id = ?", userId);
        }
    }

    private void safeDeleteByOrderFk(String table, String orderColumn, Long userId) {
        if (!tableExists(table)) {
            log.warn("Skipping cleanup for {} — table not found", table);
            return;
        }
        if (!columnExists(table, orderColumn)) {
            log.warn("Skipping cleanup for {} — column '{}' not found", table, orderColumn);
            return;
        }
        try {
            jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE " + orderColumn
                            + " IN (SELECT id FROM orders WHERE user_id = ?)",
                    userId
            );
        } catch (Exception ex) {
            log.warn(
                    "Skipping cleanup for {} while deleting userId={}: {}",
                    table,
                    userId,
                    ex.getMessage()
            );
        }
    }

    private void safeDeletePaymentsForUserOrders(Long userId) {
        String table = null;
        if (tableExists("payments")) {
            table = "payments";
        } else if (tableExists("payment")) {
            table = "payment";
        }
        if (table == null) {
            log.warn("Skipping payments cleanup — no payments/payment table for userId={}", userId);
            return;
        }

        String orderColumn = null;
        if (columnExists(table, "order_id")) {
            orderColumn = "order_id";
        } else if (columnExists(table, "orderId")) {
            orderColumn = "orderId";
        }
        if (orderColumn == null) {
            log.warn(
                    "Skipping payments cleanup — {}.order_id/orderId not found for userId={}",
                    table,
                    userId
            );
            return;
        }

        try {
            jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE " + orderColumn
                            + " IN (SELECT id FROM orders WHERE user_id = ?)",
                    userId
            );
        } catch (Exception ex) {
            log.warn("Skipping payments cleanup for userId={}: {}", userId, ex.getMessage());
        }
    }

    private void deleteUserScopedRows(Long userId, int uid) {
        safeUpdate("user_wallet_transactions", "DELETE FROM user_wallet_transactions WHERE user_id = ?", uid);
        safeUpdate("user_wallet", "DELETE FROM user_wallet WHERE user_id = ?", uid);
        safeUpdate(
                "referral_transactions",
                """
                DELETE FROM referral_transactions
                WHERE referrer_id = ? OR referred_user_id = ?
                """,
                uid,
                uid
        );
        safeUpdate("email_logs", "DELETE FROM email_logs WHERE user_id = ?", uid);
        safeUpdate("push_notifications", "DELETE FROM push_notifications WHERE user_id = ?", userId);
        safeUpdate("product_reviews", "DELETE FROM product_reviews WHERE user_id = ?", userId);
        safeUpdate("product_views", "DELETE FROM product_views WHERE user_id = ?", userId);
        safeUpdate("search_history", "DELETE FROM search_history WHERE user_id = ?", userId);

        safeUpdate(
                "cart_items",
                """
                DELETE FROM cart_items
                WHERE cart_id IN (SELECT id FROM cart WHERE user_id = ?)
                """,
                userId
        );

        safeUpdate("cart", "DELETE FROM cart WHERE user_id = ?", userId);
        safeUpdate("user_wishlist", "DELETE FROM user_wishlist WHERE user_id = ?", userId);
        safeUpdate("addresses", "DELETE FROM addresses WHERE user_id = ?", userId);
        safeUpdate("shoppers", "DELETE FROM shoppers WHERE user_id = ?", userId);
        safeUpdate("user_location", "DELETE FROM user_location WHERE user_id = ?", userId);
    }
}
