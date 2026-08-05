package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.repository.ProductEmbeddingRepository;
import com.ecommerce.authdemo.service.AIImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal endpoints to reindex catalog image embeddings for camera / visual search.
 */
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
@Slf4j
public class InternalAiController {

    private final AIImageService aiImageService;
    private final ProductEmbeddingRepository embeddingRepository;

    private final AtomicBoolean indexing = new AtomicBoolean(false);

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    @GetMapping("/embeddings/stats")
    public ResponseEntity<Map<String, Object>> embeddingStats(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Forbidden: internal service key mismatch"
            ));
        }
        long active = embeddingRepository.findByIsActive(true).size();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("activeEmbeddings", active);
        body.put("indexingInProgress", indexing.get());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/embeddings/reindex")
    public ResponseEntity<Map<String, Object>> reindexEmbeddings(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Forbidden: internal service key mismatch"
            ));
        }
        if (!indexing.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Embedding reindex already in progress"
            ));
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("[INTERNAL:AI] Starting product embedding reindex");
                aiImageService.processExistingProducts();
                log.info("[INTERNAL:AI] Product embedding reindex finished");
            } catch (Exception e) {
                log.error("[INTERNAL:AI] Product embedding reindex failed", e);
            } finally {
                indexing.set(false);
            }
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "Embedding reindex started in background");
        body.put("activeEmbeddings", embeddingRepository.findByIsActive(true).size());
        return ResponseEntity.accepted().body(body);
    }

    private boolean isAuthorized(String key) {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            // Dev-friendly: allow when key is not configured (local).
            return true;
        }
        return internalServiceKey.equals(key);
    }
}
