CREATE TABLE IF NOT EXISTS product_image_signatures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_path VARCHAR(1024) NOT NULL,
    dhash_hex CHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_image_path_signature (image_path),
    INDEX idx_product_image_signatures_product_id (product_id)
);
