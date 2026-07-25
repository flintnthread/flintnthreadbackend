package com.ecommerce.authdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures product_embeddings exists even when Flyway is behind on older VPS deploys.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEmbeddingsSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS product_embeddings (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        product_id BIGINT NOT NULL,
                        embedding_vector TEXT NOT NULL,
                        model_version VARCHAR(50) DEFAULT 'clip-vit-base-patch32',
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_product_embeddings_product_id (product_id),
                        INDEX idx_product_embeddings_active (is_active)
                    )
                    """);
            log.info("[AI] product_embeddings table ready");
        } catch (Exception e) {
            log.warn("[AI] Could not ensure product_embeddings table: {}", e.getMessage());
        }
    }
}
