package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.payload.ApiResponse;
import com.ecommerce.authdemo.repository.ProductRepository;
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
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-powered image search service
 * Directly communicates with Python AI service without circular dependencies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIImageSearchDecorator {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.model.version:clip-vit-base-patch32}")
    private String modelVersion;

    /**
     * Enhanced image search using AI similarity
     */
    public ApiResponse<SearchResponseDTO> performImageSearch(MultipartFile image, Long userId, String sessionId) {
        log.info("🤖 Starting AI-powered camera search for image: {}", image.getOriginalFilename());
        
        try {
            // Validate image
            if (image == null || image.isEmpty()) {
                return new ApiResponse<>(false, "Image file is required", null);
            }

            // Generate embedding for query image
            String queryEmbedding = generateEmbedding(image);
            if (queryEmbedding == null) {
                log.warn("⚠️ Failed to generate embedding for query image - using fallback");
                
                // Fallback: Return recent products when AI service is unavailable
                List<Product> fallbackProducts = productRepository.findTop60ByStatusOrderByCreatedAtDesc("active");
                log.info("🔄 Returning {} fallback products for camera search", fallbackProducts.size());
                
                return new ApiResponse<>(true, "Camera search completed (fallback mode)", new SearchResponseDTO(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    fallbackProducts
                ));
            }

            // Find similar products using AI
            List<Long> similarProductIds = findSimilarProductIds(queryEmbedding, 20);
            
            if (similarProductIds.isEmpty()) {
                log.warn("⚠️ No similar products found for image search - using fallback");
                
                // Fallback: Return recent products when no similar products found
                List<Product> fallbackProducts = productRepository.findTop60ByStatusOrderByCreatedAtDesc("active");
                log.info("🔄 Returning {} fallback products for camera search", fallbackProducts.size());
                
                return new ApiResponse<>(true, "Camera search completed (no similar products found)", new SearchResponseDTO(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    fallbackProducts
                ));
            }

            // Fetch product details
            List<Product> similarProducts = productRepository.findAllById(similarProductIds);
            
            log.info("✅ Found {} similar products for camera search", similarProducts.size());
            
            SearchResponseDTO response = new SearchResponseDTO(
                Collections.emptyList(),
                Collections.emptyList(),
                similarProducts
            );
            
            return new ApiResponse<>(true, "Camera search completed successfully", response);
            
        } catch (Exception e) {
            log.error("❌ Error during AI camera search", e);
            return new ApiResponse<>(false, "Camera search failed: " + e.getMessage(), null);
        }
    }

    private String generateEmbedding(MultipartFile image) {
        try {
            // Convert image to base64
            String base64Image = encodeImageToBase64(image);
            
            // Call Python AI service
            Map<String, Object> request = Map.of(
                "image", base64Image,
                "model", modelVersion
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                aiServiceUrl + "/embeddings/generate", 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("embedding")) {
                @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private List<Long> findSimilarProductIds(String queryEmbedding, int limit) {
        try {
            // Call similarity search endpoint
            Map<String, Object> request = Map.of(
                "query_embedding", queryEmbedding,
                "limit", limit
            );
            
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

    private String encodeImageToBase64(MultipartFile image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
