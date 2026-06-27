-- Run once on flintnt database (ddl-auto=none)
CREATE TABLE IF NOT EXISTS seller_preferences (
    seller_id INT NOT NULL PRIMARY KEY,
    push_notifications TINYINT(1) NOT NULL DEFAULT 1,
    order_updates TINYINT(1) NOT NULL DEFAULT 1,
    payout_alerts TINYINT(1) NOT NULL DEFAULT 1,
    vacation_mode TINYINT(1) NOT NULL DEFAULT 0,
    dark_mode TINYINT(1) NOT NULL DEFAULT 0,
    language VARCHAR(10) NOT NULL DEFAULT 'en-IN',
    biometric_login TINYINT(1) NOT NULL DEFAULT 0,
    updated_at DATETIME NULL,
    CONSTRAINT fk_seller_preferences_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
