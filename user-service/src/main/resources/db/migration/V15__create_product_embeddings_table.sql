-- Camera / visual search: CLIP embeddings per product (durable DB index).
CREATE TABLE IF NOT EXISTS product_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    embedding_vector TEXT NOT NULL,
    model_version VARCHAR(50) DEFAULT 'clip-vit-base-patch32',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_embeddings_product_id (product_id),
    INDEX idx_product_embeddings_active (is_active)
);
