package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductEmbedding;
import com.ecommerce.authdemo.payload.ApiResponse;
import com.ecommerce.authdemo.repository.ProductEmbeddingRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI-powered image search — returns visually similar products only (never generic catalog fallback).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIImageSearchDecorator {

    private static final int SIMILAR_PRODUCT_LIMIT = 20;

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.model.version:clip-vit-base-patch32}")
    private String modelVersion;

    @Transactional(readOnly = true)
    public ApiResponse<SearchResponseDTO> performImageSearch(MultipartFile image, Long userId, String sessionId) {
        log.info("Starting AI camera search for image: {}", image != null ? image.getOriginalFilename() : "null");

        try {
            if (image == null || image.isEmpty()) {
                return new ApiResponse<>(false, "Image file is required", null);
            }

            String queryEmbedding = generateEmbedding(image);
            if (queryEmbedding == null) {
                log.warn("Failed to generate embedding for query image");
                return new ApiResponse<>(
                        false,
                        "Unable to analyze this image right now. Please try a clear product photo.",
                        null
                );
            }

            List<Long> similarProductIds = findSimilarProductIds(queryEmbedding, SIMILAR_PRODUCT_LIMIT);
            if (similarProductIds.isEmpty()) {
                similarProductIds = findSimilarProductIdsFromDatabase(queryEmbedding, SIMILAR_PRODUCT_LIMIT);
            }

            if (similarProductIds.isEmpty()) {
                log.info("No similar products found for uploaded image");
                return new ApiResponse<>(
                        true,
                        "No similar products found for this image.",
                        new SearchResponseDTO(
                                Collections.emptyList(),
                                Collections.emptyList(),
                                Collections.emptyList()
                        )
                );
            }

            List<Product> similarProducts = loadProductsInOrder(similarProductIds);
            log.info("Found {} similar products for camera search", similarProducts.size());

            SearchResponseDTO response = new SearchResponseDTO(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    similarProducts
            );

            return new ApiResponse<>(true, "Camera search completed successfully", response);

        } catch (Exception e) {
            log.error("Error during AI camera search", e);
            return new ApiResponse<>(false, "Camera search failed: " + e.getMessage(), null);
        }
    }

    private List<Product> loadProductsInOrder(List<Long> similarProductIds) {
        Map<Long, Product> byId = productRepository.findAllById(similarProductIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a, LinkedHashMap::new));

        List<Product> ordered = new ArrayList<>();
        for (Long id : similarProductIds) {
            Product product = byId.get(id);
            if (product != null) {
                ordered.add(product);
            }
        }
        return ordered;
    }

    private String generateEmbedding(MultipartFile image) {
        try {
            String base64Image = encodeImageToBase64(image);

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
            log.error("Error finding similar products via AI service", e);
            return Collections.emptyList();
        }
    }

    private List<Long> findSimilarProductIdsFromDatabase(String queryEmbedding, int limit) {
        double[] queryVector = parseEmbeddingVector(queryEmbedding);
        if (queryVector.length == 0) {
            return Collections.emptyList();
        }

        List<ProductEmbedding> storedEmbeddings = embeddingRepository.findByIsActive(true);
        if (storedEmbeddings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<Long, Double>> scored = new ArrayList<>();
        for (ProductEmbedding stored : storedEmbeddings) {
            if (stored.getProduct() == null || stored.getEmbeddingVector() == null) {
                continue;
            }
            double[] candidate = parseEmbeddingVector(stored.getEmbeddingVector());
            if (candidate.length != queryVector.length) {
                continue;
            }
            double similarity = cosineSimilarity(queryVector, candidate);
            if (similarity > 0) {
                scored.add(Map.entry(stored.getProduct().getId(), similarity));
            }
        }

        scored.sort(Comparator.comparingDouble(Map.Entry<Long, Double>::getValue).reversed());
        return scored.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double[] parseEmbeddingVector(String csv) {
        if (csv == null || csv.isBlank()) {
            return new double[0];
        }
        String[] parts = csv.split(",");
        double[] vector = new double[parts.length];
        int length = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                vector[length++] = Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                return new double[0];
            }
        }
        if (length == parts.length) {
            return vector;
        }
        double[] resized = new double[length];
        System.arraycopy(vector, 0, resized, 0, length);
        return resized;
    }

    private double cosineSimilarity(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String encodeImageToBase64(MultipartFile image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
        if (bufferedImage == null) {
            throw new IOException("Unsupported or unreadable image format");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
