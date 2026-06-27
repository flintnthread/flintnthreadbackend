CREATE TABLE IF NOT EXISTS seller_bank_edit_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    old_bank_name VARCHAR(100) NULL,
    old_account_number VARCHAR(50) NULL,
    old_ifsc_code VARCHAR(20) NULL,
    old_account_holder VARCHAR(100) NULL,
    old_branch_name VARCHAR(100) NULL,
    new_bank_name VARCHAR(100) NOT NULL,
    new_account_number VARCHAR(50) NOT NULL,
    new_ifsc_code VARCHAR(20) NOT NULL,
    new_account_holder VARCHAR(100) NOT NULL,
    new_branch_name VARCHAR(100) NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    admin_note VARCHAR(500) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    approved_by_admin_id BIGINT NULL,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),

    CONSTRAINT fk_bank_edit_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(id)
);
