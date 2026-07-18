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
import java.sql.SQLException;

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
            return product != null && product.toLowerCase().contains("h2");
        } catch (SQLException ex) {
            log.warn("Could not detect database product, assuming MySQL: {}", ex.getMessage());
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
        try {
            jdbcTemplate.update(sql, args);
        } catch (Exception ex) {
            log.warn("Skipping cleanup for {} while deleting account: {}", label, ex.getMessage());
        }
    }

    private void deleteOrderData(Long userId) {
        jdbcTemplate.update("UPDATE orders SET address_id = NULL WHERE user_id = ?", userId);

        // Optional / legacy tables — skip when missing so account delete still succeeds.
        safeDeleteByUserOrders(
                "shiprocket_webhooks",
                "DELETE FROM shiprocket_webhooks WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        safeDeleteByUserOrders(
                "shiprocket_sync_logs",
                "DELETE FROM shiprocket_sync_logs WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        safeDeleteByUserOrders(
                "invoices",
                "DELETE FROM invoices WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        safeDeleteByUserOrders(
                "payment_transactions",
                "DELETE FROM payment_transactions WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        // `payments` may be absent or lack order_id on older schemas.
        safeDeletePaymentsForUserOrders(userId);
        safeDeleteByUserOrders(
                "seller_payment_invoices",
                "DELETE FROM seller_payment_invoices WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        safeDeleteByUserOrders(
                "wallet_transactions",
                "DELETE FROM wallet_transactions WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );
        safeDeleteByUserOrders(
                "order_status_history",
                "DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM order_items
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update("DELETE FROM orders WHERE user_id = ?", userId);
    }

    private void safeDeleteByUserOrders(String tableLabel, String sql, Long userId) {
        try {
            jdbcTemplate.update(sql, userId);
        } catch (Exception ex) {
            log.warn(
                    "Skipping cleanup for {} while deleting userId={}: {}",
                    tableLabel,
                    userId,
                    ex.getMessage()
            );
        }
    }

    private void safeDeletePaymentsForUserOrders(Long userId) {
        String[] candidates = {
                "DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
                "DELETE FROM payments WHERE orderId IN (SELECT id FROM orders WHERE user_id = ?)",
                "DELETE FROM payment WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)",
        };
        for (String sql : candidates) {
            try {
                jdbcTemplate.update(sql, userId);
                return;
            } catch (Exception ex) {
                log.debug("payments cleanup attempt failed for userId={}: {}", userId, ex.getMessage());
            }
        }
        log.warn("No compatible payments table cleanup succeeded for userId={}", userId);
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
