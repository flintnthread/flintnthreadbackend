-- Run once in MySQL (shopping database) for live support chat
CREATE TABLE IF NOT EXISTS seller_live_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    sender_type VARCHAR(20) NOT NULL COMMENT 'seller, bot, admin',
    message TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_seller_chat_created (seller_id, created_at)
);
