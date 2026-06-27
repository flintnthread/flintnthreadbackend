CREATE TABLE IF NOT EXISTS seller_payout_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    seller_note VARCHAR(500) NULL,
    admin_note VARCHAR(500) NULL,
    transaction_ref VARCHAR(100) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    paid_at DATETIME NULL,
    reviewed_by_admin_id BIGINT NULL,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    INDEX idx_seller_id (seller_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),

    CONSTRAINT fk_payout_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(id),

    CONSTRAINT fk_payout_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
);
