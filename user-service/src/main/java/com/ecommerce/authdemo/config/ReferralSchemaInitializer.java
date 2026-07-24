package com.ecommerce.authdemo.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures referral-related columns exist when {@code ddl-auto=none}.
 */
@Component
@RequiredArgsConstructor
public class ReferralSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReferralSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureReferralColumns() {
        try {
            ensureOrdersReferralColumn();
            ensureUsersDiscountAvailableColumn();
        } catch (Exception error) {
            log.error(
                    "[REFERRAL] Failed to ensure referral schema. "
                            + "Run src/main/resources/db/referral_schema_update.sql manually: {}",
                    error.getMessage(),
                    error
            );
        }
    }

    private void ensureOrdersReferralColumn() {
        if (columnExists("orders", "referral_inviter_discount_applied")) {
            log.info("[REFERRAL] orders.referral_inviter_discount_applied already exists");
            return;
        }

        // Do not ALTER orders — column is absent on live DB; Order entity maps it as @Transient.
        log.warn(
                "[REFERRAL] orders.referral_inviter_discount_applied is not in the live DB; "
                        + "skipping ADD COLUMN (entity uses @Transient)"
        );
    }

    private void ensureUsersDiscountAvailableColumn() {
        if (!columnExists("users", "discount_available")) {
            log.warn(
                    "[REFERRAL] users.discount_available is missing; skipping ADD COLUMN"
            );
            return;
        }

        if (
                columnExists("users", "reward_unlocked")
                        && columnExists("users", "first_order_completed")
                        && columnExists("users", "discount_available")
        ) {
            jdbcTemplate.update(
                    """
                    UPDATE users u
                    SET u.discount_available = TRUE
                    WHERE u.reward_unlocked = TRUE
                      AND (u.first_order_completed IS NULL OR u.first_order_completed = FALSE)
                      AND (u.discount_available IS NULL OR u.discount_available = FALSE)
                    """
            );
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }
}
