-- Run manually if your sellers table is missing registration / verification columns.

ALTER TABLE sellers
    ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS email_verified_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS mobile_verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mobile_verified_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS otp_sent_at DATETIME NULL;
