package com.ecommerce.adminbackend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls user-service internal Shiprocket push/sync endpoints.
 */
@Component
@Slf4j
public class UserServiceShiprocketClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.user-service-url:http://127.0.0.1:8080}")
    private String userServiceUrl;

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    public Map<String, Object> pushOrder(Long orderId) {
        return post("/api/internal/shiprocket/orders/" + orderId + "/push", "push", orderId);
    }

    public Map<String, Object> syncOrder(Long orderId) {
        return post("/api/internal/shiprocket/orders/" + orderId + "/sync", "sync", orderId);
    }

    private Map<String, Object> post(String path, String action, Long orderId) {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            throw new IllegalStateException(
                    "app.internal-service-key is not configured on admin-service"
            );
        }
        String base = userServiceUrl == null ? "http://127.0.0.1:8080" : userServiceUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + path;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(90))
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            log.info("Admin Shiprocket {} orderId={} url={}", action, orderId, url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseBody(response.body());
            log.info(
                    "Admin Shiprocket {} response orderId={} status={} body={}",
                    action,
                    orderId,
                    response.statusCode(),
                    response.body()
            );
            if (response.statusCode() >= 400) {
                String message = extractMessage(body, response.body());
                if (message != null && message.toLowerCase().contains("unknown column")) {
                    throw new IllegalStateException(
                            "user-service error: " + message
                    );
                }
                throw new IllegalStateException(message);
            }
            if (body.containsKey("success") && Boolean.FALSE.equals(body.get("success"))) {
                throw new IllegalStateException(extractMessage(body, response.body()));
            }
            return body;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Shiprocket " + action + " failed: " + e.getMessage(),
                    e
            );
        }
    }

    private Map<String, Object> parseBody(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("message", raw);
            return fallback;
        }
    }

    private static String extractMessage(Map<String, Object> body, String raw) {
        if (body != null) {
            Object detail = body.get("shipping_error_detail");
            if (detail != null && !String.valueOf(detail).isBlank()) {
                return String.valueOf(detail);
            }
            Object message = body.get("message");
            if (message != null && !String.valueOf(message).isBlank()) {
                return String.valueOf(message);
            }
        }
        return raw != null && !raw.isBlank() ? raw : "Shiprocket request failed";
    }
}
