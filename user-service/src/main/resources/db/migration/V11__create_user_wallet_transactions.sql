CREATE TABLE IF NOT EXISTS user_wallet_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_id INT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uwt_user_id (user_id),
    INDEX idx_uwt_order_id (order_id)
);
