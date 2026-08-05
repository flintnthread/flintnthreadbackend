package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductEmbedding;
import com.ecommerce.authdemo.payload.ApiResponse;
import com.ecommerce.authdemo.repository.ProductEmbeddingRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.util.EmbeddingVectorMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
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
        try {
            if (image == null || image.isEmpty()) {
                return new ApiResponse<>(false, "Image file is required", null);
            }
            return performImageSearchFromBytes(image.getBytes(), image.getOriginalFilename());
        } catch (Exception e) {
            log.error("Error during AI camera search", e);
            // Soft-fail so local visual search in SearchServiceImpl can still run.
            return new ApiResponse<>(
                    true,
                    "AI visual search unavailable; using local matching.",
                    new SearchResponseDTO(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<SearchResponseDTO> performImageSearchFromBytes(byte[] imageBytes, String filename) {
        log.info("Starting AI camera search for image: {}", filename);

        try {
            if (imageBytes == null || imageBytes.length == 0) {
                return new ApiResponse<>(false, "Image file is required", null);
            }

            String queryEmbedding = generateEmbeddingFromBytes(imageBytes);
            if (queryEmbedding == null) {
                log.warn("Failed to generate embedding for query image — AI service may be down");
                return new ApiResponse<>(
                        true,
                        "AI visual search unavailable; using local matching.",
                        new SearchResponseDTO(
                                Collections.emptyList(),
                                Collections.emptyList(),
                                Collections.emptyList()
                        )
                );
            }

            // Prefer durable DB embeddings; Python in-memory index is optional warm cache.
            List<Long> similarProductIds = findSimilarProductIdsFromDatabase(queryEmbedding, SIMILAR_PRODUCT_LIMIT);
            if (similarProductIds.isEmpty()) {
                similarProductIds = findSimilarProductIds(queryEmbedding, SIMILAR_PRODUCT_LIMIT);
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
            return new ApiResponse<>(
                    true,
                    "AI visual search unavailable; using local matching.",
                    new SearchResponseDTO(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );
        }
    }

    private List<Product> loadProductsInOrder(List<Long> similarProductIds) {
        if (similarProductIds == null || similarProductIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> byId = productRepository.findAllWithImagesAndVariantsByIdIn(similarProductIds).stream()
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

    private String generateEmbeddingFromBytes(byte[] imageBytes) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                throw new IOException("Unsupported or unreadable image format");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

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
            log.error("Error generating embedding: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> findSimilarProductIds(String queryEmbedding, int limit) {
        try {
            Map<String, Object> request = Map.of(
                    "query_embedding", queryEmbedding,
                    "limit", limit,
                    "min_similarity", EmbeddingVectorMath.MIN_CAMERA_SIMILARITY
            );

            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + "/embeddings/similarity-search",
                    request,
                    Map.class
            );

            if (response != null && response.get("similar_product_ids") instanceof List<?> ids) {
                List<?> similarities = response.get("similarities") instanceof List<?> sims
                        ? (List<?>) sims
                        : List.of();
                List<Long> result = new ArrayList<>();
                for (int i = 0; i < ids.size(); i++) {
                    Object v = ids.get(i);
                    Long id = null;
                    if (v instanceof Number n) {
                        id = n.longValue();
                    } else {
                        try {
                            id = Long.parseLong(String.valueOf(v).trim());
                        } catch (NumberFormatException ignored) {
                            // skip
                        }
                    }
                    if (id == null || id <= 0) {
                        continue;
                    }
                    if (i < similarities.size()) {
                        double score = toDouble(similarities.get(i));
                        if (score < EmbeddingVectorMath.MIN_CAMERA_SIMILARITY) {
                            continue;
                        }
                    }
                    result.add(id);
                }
                return result;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error finding similar products via AI service", e);
            return Collections.emptyList();
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Long> findSimilarProductIdsFromDatabase(String queryEmbedding, int limit) {
        double[] queryVector = EmbeddingVectorMath.parseCsv(queryEmbedding);
        if (queryVector.length == 0) {
            return Collections.emptyList();
        }

        List<ProductEmbedding> storedEmbeddings = embeddingRepository.findByIsActive(true);
        if (storedEmbeddings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> catalog = new LinkedHashMap<>();
        for (ProductEmbedding stored : storedEmbeddings) {
            if (stored.getProduct() == null || stored.getProduct().getId() == null || stored.getEmbeddingVector() == null) {
                continue;
            }
            catalog.put(stored.getProduct().getId(), stored.getEmbeddingVector());
        }
        return EmbeddingVectorMath.topSimilarProductIds(queryVector, catalog, limit);
    }
}
