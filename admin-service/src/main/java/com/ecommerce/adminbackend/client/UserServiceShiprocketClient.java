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
                    "app.internal-service-key is not configured on admin-service. "
                            + "Set INTERNAL_SERVICE_KEY in /etc/flintnthread/application.properties "
                            + "(same value on user-service)."
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
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            log.info("Admin Shiprocket {} orderId={} url={}", action, orderId, url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String raw = response.body() != null ? response.body() : "";
            Map<String, Object> body = parseBody(raw);
            log.info(
                    "Admin Shiprocket {} response orderId={} status={} body={}",
                    action,
                    orderId,
                    response.statusCode(),
                    raw.length() > 2000 ? raw.substring(0, 2000) : raw
            );
            if (response.statusCode() == 403) {
                throw new IllegalStateException(
                        "user-service rejected internal key (HTTP 403). "
                                + "Set the same INTERNAL_SERVICE_KEY on admin-service and user-service."
                );
            }
            if (response.statusCode() == 404) {
                throw new IllegalStateException(
                        "user-service Shiprocket endpoint not found (HTTP 404). "
                                + "Redeploy user-service so /api/internal/shiprocket/orders/{id}/" + action + " exists."
                );
            }
            if (response.statusCode() >= 400) {
                String message = extractMessage(body, raw);
                throw new IllegalStateException(
                        "user-service HTTP " + response.statusCode() + ": " + message
                );
            }
            if (body.containsKey("success") && Boolean.FALSE.equals(asBoolean(body.get("success")))) {
                throw new IllegalStateException(extractMessage(body, raw));
            }
            return body;
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Shiprocket " + action + " interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Shiprocket " + action + " failed: " + rootMessage(e),
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
            fallback.put("message", raw.length() > 500 ? raw.substring(0, 500) : raw);
            return fallback;
        }
    }

    private static String extractMessage(Map<String, Object> body, String raw) {
        if (body != null) {
            Object detail = body.get("shipping_error_detail");
            if (detail != null && !String.valueOf(detail).isBlank()
                    && !"null".equalsIgnoreCase(String.valueOf(detail))) {
                return String.valueOf(detail);
            }
            Object message = body.get("message");
            if (message != null && !String.valueOf(message).isBlank()
                    && !"null".equalsIgnoreCase(String.valueOf(message))) {
                return String.valueOf(message);
            }
        }
        if (raw != null && !raw.isBlank()) {
            return raw.length() > 500 ? raw.substring(0, 500) : raw;
        }
        return "Shiprocket request failed (empty response from user-service)";
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        String fallback = e.getClass().getSimpleName();
        while (cur != null) {
            if (cur.getMessage() != null && !cur.getMessage().isBlank()
                    && !"null".equalsIgnoreCase(cur.getMessage().trim())) {
                return cur.getMessage();
            }
            fallback = cur.getClass().getSimpleName();
            cur = cur.getCause();
        }
        return fallback;
    }
}
