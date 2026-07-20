package com.ecommerce.authdemo.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures OTP-auth columns exist when {@code ddl-auto=none} (no Flyway auto-apply).
 *
 * <p>Adds {@code users.mobile_verified} / {@code users.email_verified} for the
 * full-registration flow, and {@code otp.purpose} / {@code otp.created_at} plus a
 * widened {@code otp.otp} column so BCrypt hashes fit. Legacy accounts are left
 * as {@code mobile_verified = FALSE} on purpose so mobile uniqueness only applies
 * to newly fully-registered accounts (protects Switch Account).
 */
@Component
@RequiredArgsConstructor
public class AuthSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AuthSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAuthColumns() {
        try {
            ensureUsersVerificationColumns();
            ensureOtpColumns();
        } catch (Exception error) {
            log.error(
                    "[AUTH-SCHEMA] Failed to ensure OTP auth schema. "
                            + "Apply the users.mobile_verified/email_verified and otp.purpose/created_at "
                            + "columns manually if needed: {}",
                    error.getMessage(),
                    error
            );
        }
    }

    private void ensureUsersVerificationColumns() {
        if (!columnExists("users", "mobile_verified")) {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN mobile_verified BOOLEAN DEFAULT FALSE"
            );
            log.info("[AUTH-SCHEMA] Added users.mobile_verified");
        }

        if (!columnExists("users", "email_verified")) {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE"
            );
            log.info("[AUTH-SCHEMA] Added users.email_verified");

            // Backfill: real (non-synthetic) emails on already-verified accounts are
            // treated as email-verified. Mobile stays FALSE for legacy accounts.
            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET email_verified = TRUE
                    WHERE (email_verified IS NULL OR email_verified = FALSE)
                      AND email IS NOT NULL
                      AND email NOT LIKE '%@mobile.flintnthread.in'
                      AND email NOT LIKE '%@mobile.flintnthread.online'
                      AND email NOT REGEXP '^[0-9]{10}@'
                    """
            );
        }
    }

    private void ensureOtpColumns() {
        if (!tableExists("otp")) {
            log.info("[AUTH-SCHEMA] otp table not found yet; skipping otp column checks");
            return;
        }

        if (!columnExists("otp", "purpose")) {
            jdbcTemplate.execute("ALTER TABLE otp ADD COLUMN purpose VARCHAR(20)");
            log.info("[AUTH-SCHEMA] Added otp.purpose");
        }

        if (!columnExists("otp", "created_at")) {
            jdbcTemplate.execute("ALTER TABLE otp ADD COLUMN created_at DATETIME");
            log.info("[AUTH-SCHEMA] Added otp.created_at");
        }

        // BCrypt hashes are 60 chars; make sure the column can hold them.
        try {
            jdbcTemplate.execute("ALTER TABLE otp MODIFY otp VARCHAR(255)");
        } catch (Exception e) {
            log.warn("[AUTH-SCHEMA] Could not widen otp.otp column: {}", e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                table
        );
        return count != null && count > 0;
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
