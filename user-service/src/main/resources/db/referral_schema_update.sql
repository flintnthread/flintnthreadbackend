-- Referral system — apply manually if columns are missing (ddl-auto=none).
-- If a column already exists, skip that statement or remove the error line.

ALTER TABLE users
    ADD COLUMN discount_available BOOLEAN DEFAULT FALSE;

UPDATE users u
SET u.discount_available = TRUE
WHERE u.reward_unlocked = TRUE
  AND (u.first_order_completed IS NULL OR u.first_order_completed = FALSE);

ALTER TABLE orders
    ADD COLUMN referral_inviter_discount_applied BOOLEAN DEFAULT FALSE;
