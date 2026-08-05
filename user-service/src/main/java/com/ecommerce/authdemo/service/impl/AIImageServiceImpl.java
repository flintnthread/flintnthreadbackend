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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIImageServiceImpl implements AIImageService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;

    @Value("${ai.service.url:http://127.0.0.1:5000}")
    private String aiServiceUrl;

    @Value("${ai.model.version:clip-vit-base-patch32}")
    private String modelVersion;

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
    }

    @Override
    public String generateEmbedding(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return null;
            }
            return generateEmbeddingFromBytes(image.getBytes(), image.getContentType());
        } catch (Exception e) {
            log.error("Error generating embedding from multipart", e);
            return null;
        }
    }

    public String generateEmbeddingFromBytes(byte[] imageBytes, String contentType) {
        try {
            if (imageBytes == null || imageBytes.length == 0) {
                return null;
            }
            // Normalize to JPEG so CLIP always gets a decodeable RGB image.
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(imageBytes));
            byte[] jpegBytes = imageBytes;
            if (buffered != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(buffered, "jpg", baos);
                jpegBytes = baos.toByteArray();
            }
            String base64Image = Base64.getEncoder().encodeToString(jpegBytes);

            Map<String, Object> request = new HashMap<>();
            request.put("image", base64Image);
            request.put("model", modelVersion);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + "/embeddings/generate",
                    request,
                    Map.class
            );

            if (response != null && response.get("embedding") instanceof List<?> embedding) {
                return embedding.stream().map(Object::toString).collect(Collectors.joining(","));
            }

            log.warn("AI service returned no embedding");
            return null;
        } catch (Exception e) {
            log.error("Error generating embedding from bytes: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<Long> findSimilarProductIds(MultipartFile queryImage, int limit) {
        try {
            String queryEmbedding = generateEmbedding(queryImage);
            if (queryEmbedding == null) {
                return Collections.emptyList();
            }

            Map<String, Object> request = new HashMap<>();
            request.put("query_embedding", queryEmbedding);
            request.put("limit", limit);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + "/embeddings/similarity-search",
                    request,
                    Map.class
            );

            if (response != null && response.get("similar_product_ids") instanceof List<?> ids) {
                return ids.stream().map(AIImageServiceImpl::toLongId).filter(id -> id != null && id > 0).toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error finding similar products via AI service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void processExistingProducts() {
        log.info("[AI] Indexing product embeddings (active catalog)");
        List<Product> products = productRepository.findTop300ByStatusOrderByCreatedAtDesc("active");
        int ok = 0;
        int skipped = 0;
        int failed = 0;

        for (Product product : products) {
            try {
                if (product.getId() == null) {
                    skipped++;
                    continue;
                }
                if (embeddingRepository.findByProduct_IdAndIsActive(product.getId(), true).isPresent()) {
                    skipped++;
                    continue;
                }
                boolean indexed = indexProductFromCatalogImage(product);
                if (indexed) {
                    ok++;
                } else {
                    failed++;
                }
                if ((ok + failed) % 25 == 0) {
                    log.info("[AI] Embedding progress indexed={} failed={} skipped={}", ok, failed, skipped);
                }
            } catch (Exception e) {
                failed++;
                log.warn("[AI] Embedding failed productId={}: {}", product.getId(), e.getMessage());
            }
        }

        log.info("[AI] Embedding indexing done indexed={} failed={} skipped={} total={}",
                ok, failed, skipped, products.size());
    }

    @Override
    @Transactional
    public void updateProductEmbedding(Long productId, MultipartFile image) {
        try {
            String embedding = generateEmbedding(image);
            if (embedding == null) {
                log.error("Failed to generate embedding for product {}", productId);
                return;
            }
            saveEmbedding(productId, embedding);
        } catch (Exception e) {
            log.error("Error updating embedding for product {}", productId, e);
        }
    }

    /** Index one product from its primary catalog image URL. */
    @Transactional
    public boolean indexProductFromCatalogImage(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }
        String imageUrl = resolvePrimaryImageUrl(product);
        if (!StringUtils.hasText(imageUrl)) {
            log.debug("[AI] No image for product {}", product.getId());
            return false;
        }
        byte[] bytes = downloadImageBytes(imageUrl);
        if (bytes == null || bytes.length == 0) {
            log.warn("[AI] Could not download image for product {} url={}", product.getId(), imageUrl);
            return false;
        }
        String embedding = generateEmbeddingFromBytes(bytes, "image/jpeg");
        if (embedding == null) {
            return false;
        }
        saveEmbedding(product.getId(), embedding);
        // Best-effort warm Python in-memory index (optional; DB is source of truth).
        tryStoreInAiService(product.getId(), embedding);
        return true;
    }

    private void saveEmbedding(Long productId, String embedding) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        ProductEmbedding embeddingRecord = embeddingRepository
                .findByProduct_IdAndIsActive(productId, true)
                .orElseGet(ProductEmbedding::new);

        embeddingRecord.setProduct(product);
        embeddingRecord.setEmbeddingVector(embedding);
        embeddingRecord.setModelVersion(modelVersion);
        embeddingRecord.setIsActive(true);
        embeddingRepository.save(embeddingRecord);
        log.info("[AI] Saved embedding for product {}", productId);
    }

    private void tryStoreInAiService(Long productId, String embeddingCsv) {
        try {
            List<Double> vector = java.util.Arrays.stream(embeddingCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Double::valueOf)
                    .toList();
            Map<String, Object> body = new HashMap<>();
            body.put("product_id", productId);
            body.put("embedding", vector);
            restTemplate.postForObject(aiServiceUrl + "/embeddings/store", body, Map.class);
        } catch (Exception e) {
            log.debug("[AI] Optional store to Python skipped: {}", e.getMessage());
        }
    }

    private String resolvePrimaryImageUrl(Product product) {
        Set<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            return null;
        }
        Optional<ProductImage> primary = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst();
        ProductImage chosen = primary.orElseGet(() -> images.iterator().next());
        return resolveImageUrl(chosen.getImagePath());
    }

    private String resolveImageUrl(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String path = rawPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = StringUtils.hasText(mediaPublicBaseUrl) ? mediaPublicBaseUrl.trim() : "";
        if (!StringUtils.hasText(base)) {
            return null;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    private byte[] downloadImageBytes(String url) {
        try {
            URLConnection conn = URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(20_000);
            conn.setRequestProperty("User-Agent", "FlintnThread-CameraSearch/1.0");
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.warn("[AI] Download failed url={}: {}", url, e.getMessage());
            return null;
        }
    }

    private static Long toLongId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
