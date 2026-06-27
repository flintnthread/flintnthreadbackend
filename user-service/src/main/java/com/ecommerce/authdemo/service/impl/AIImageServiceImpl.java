package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductEmbedding;
import com.ecommerce.authdemo.entity.ProductImage;
import com.ecommerce.authdemo.repository.ProductEmbeddingRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.service.AIImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIImageServiceImpl implements AIImageService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.model.version:clip-vit-base-patch32}")
    private String modelVersion;

    @Override
    public String generateEmbedding(MultipartFile image) {
        try {
            // Convert image to base64
            String base64Image = encodeImageToBase64(image);
            
            // Call Python AI service
            Map<String, Object> request = new HashMap<>();
            request.put("image", base64Image);
            request.put("model", modelVersion);
            
            Map<String, Object> response = restTemplate.postForObject(
                aiServiceUrl + "/embeddings/generate", 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("embedding")) {
                List<Double> embedding = (List<Double>) response.get("embedding");
                return embedding.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            }
            
            log.error("Failed to generate embedding from AI service");
            return null;
            
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            return null;
        }
    }

    @Override
    public List<Long> findSimilarProductIds(MultipartFile queryImage, int limit) {
        try {
            // Generate embedding for query image
            String queryEmbedding = generateEmbedding(queryImage);
            if (queryEmbedding == null) {
                return Collections.emptyList();
            }
            
            // Call similarity search endpoint
            Map<String, Object> request = new HashMap<>();
            request.put("query_embedding", queryEmbedding);
            request.put("limit", limit);
            
            Map<String, Object> response = restTemplate.postForObject(
                aiServiceUrl + "/embeddings/similarity-search", 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("similar_product_ids")) {
                return ((List<Integer>) response.get("similar_product_ids")).stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error finding similar products", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void processExistingProducts() {
        log.info("Starting to process existing products for embeddings");
        
        List<Product> products = productRepository.findAll();
        int processed = 0;
        
        for (Product product : products) {
            try {
                // Get primary image for product
                Optional<ProductImage> primaryImage = product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst();
                
                if (primaryImage.isPresent()) {
                    // For now, we'll skip actual image processing since we need the actual file
                    // In production, you'd fetch the image from Cloudinary
                    log.info("Would process embedding for product: {}", product.getId());
                }
                
                processed++;
                if (processed % 100 == 0) {
                    log.info("Processed {} products", processed);
                }
            } catch (Exception e) {
                log.error("Error processing product {}: {}", product.getId(), e.getMessage());
            }
        }
        
        log.info("Completed processing {} products", processed);
    }

    @Override
    public void updateProductEmbedding(Long productId, MultipartFile image) {
        try {
            // Generate new embedding
            String embedding = generateEmbedding(image);
            if (embedding == null) {
                log.error("Failed to generate embedding for product {}", productId);
                return;
            }
            
            // Get or create embedding record
            ProductEmbedding embeddingRecord = embeddingRepository
                .findByProductIdAndIsActive(productId, true)
                .orElse(new ProductEmbedding());
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            embeddingRecord.setProduct(product);
            embeddingRecord.setEmbeddingVector(embedding);
            embeddingRecord.setModelVersion(modelVersion);
            embeddingRecord.setIsActive(true);
            
            embeddingRepository.save(embeddingRecord);
            log.info("Updated embedding for product {}", productId);
            
        } catch (Exception e) {
            log.error("Error updating embedding for product {}", productId, e);
        }
    }

    private String encodeImageToBase64(MultipartFile image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
