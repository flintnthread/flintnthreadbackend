ALTER TABLE seller_preferences
    ADD COLUMN payout_alerts TINYINT(1) NOT NULL DEFAULT 1 AFTER order_updates,
    ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'en-IN' AFTER dark_mode;
