package com.ecommerce.authdemo.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AIImageService {

    /**
     * Generate embedding vector for product image
     * This should be called when product is uploaded
     */
    String generateEmbedding(MultipartFile image);

    /**
     * Find similar products based on image embedding
     * This is the core camera search functionality
     */
    List<Long> findSimilarProductIds(MultipartFile queryImage, int limit);

    /**
     * Batch process existing products to generate embeddings
     * Call this once to migrate existing products
     */
    void processExistingProducts();

    /**
     * Update embedding for a specific product
     */
    void updateProductEmbedding(Long productId, MultipartFile image);
}
