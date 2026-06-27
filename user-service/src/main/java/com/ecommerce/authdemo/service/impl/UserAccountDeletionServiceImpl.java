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
        jdbcTemplate.update(
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

        jdbcTemplate.update(
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

        jdbcTemplate.update(
                """
                DELETE FROM ticket_responses
                WHERE ticket_id IN (
                    SELECT id FROM support_tickets WHERE customer_id = ?
                )
                """,
                uid
        );

        jdbcTemplate.update(
                """
                DELETE FROM support_tickets
                WHERE order_id IN (
                    SELECT id FROM orders WHERE user_id = ?
                )
                """,
                userId
        );

        jdbcTemplate.update("DELETE FROM support_tickets WHERE customer_id = ?", uid);
    }

    private void deleteOrderData(Long userId) {
        jdbcTemplate.update("UPDATE orders SET address_id = NULL WHERE user_id = ?", userId);

        jdbcTemplate.update(
                """
                DELETE FROM shiprocket_webhooks
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM shiprocket_sync_logs
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM invoices
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM payment_transactions
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM payments
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM seller_payment_invoices
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM wallet_transactions
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update(
                """
                DELETE FROM order_status_history
                WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)
                """,
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

    private void deleteUserScopedRows(Long userId, int uid) {
        jdbcTemplate.update("DELETE FROM user_wallet_transactions WHERE user_id = ?", uid);
        jdbcTemplate.update("DELETE FROM user_wallet WHERE user_id = ?", uid);
        jdbcTemplate.update(
                """
                DELETE FROM referral_transactions
                WHERE referrer_id = ? OR referred_user_id = ?
                """,
                uid,
                uid
        );
        jdbcTemplate.update("DELETE FROM email_logs WHERE user_id = ?", uid);
        jdbcTemplate.update("DELETE FROM push_notifications WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM product_reviews WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM product_views WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM search_history WHERE user_id = ?", userId);

        jdbcTemplate.update(
                """
                DELETE FROM cart_items
                WHERE cart_id IN (SELECT id FROM cart WHERE user_id = ?)
                """,
                userId
        );

        jdbcTemplate.update("DELETE FROM cart WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_wishlist WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM addresses WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM shoppers WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_location WHERE user_id = ?", userId);
    }
}
