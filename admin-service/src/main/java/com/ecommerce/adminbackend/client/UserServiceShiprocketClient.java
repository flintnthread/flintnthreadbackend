package com.ecommerce.adminbackend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls user-service internal Shiprocket push/sync endpoints over local HTTP.
 * Uses RestTemplate + HTTP/1.1-style simple factory to avoid JDK HttpClient
 * ClosedChannelException issues seen on the VPS.
 */
@Component
@Slf4j
public class UserServiceShiprocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${app.user-service-url:http://127.0.0.1:8080}")
    private String userServiceUrl;

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    public UserServiceShiprocketClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

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
        String base = normalizeBase(userServiceUrl);
        if (!base.startsWith("http://127.0.0.1") && !base.startsWith("http://localhost")) {
            log.warn(
                    "Shiprocket {} using non-local user-service URL {}. Prefer USER_SERVICE_URL=http://127.0.0.1:8080 on VPS.",
                    action,
                    base
            );
        }
        String url = base + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Service-Key", internalServiceKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        Exception last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                log.info("Admin Shiprocket {} orderId={} url={} attempt={}", action, orderId, url, attempt);
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                String raw = response.getBody() != null ? response.getBody() : "";
                Map<String, Object> body = parseBody(raw);
                log.info(
                        "Admin Shiprocket {} response orderId={} status={} body={}",
                        action,
                        orderId,
                        response.getStatusCode().value(),
                        raw.length() > 2000 ? raw.substring(0, 2000) : raw
                );
                if (body.containsKey("success") && Boolean.FALSE.equals(asBoolean(body.get("success")))) {
                    throw new IllegalStateException(extractMessage(body, raw));
                }
                return body;
            } catch (HttpStatusCodeException httpEx) {
                int status = httpEx.getStatusCode().value();
                String raw = httpEx.getResponseBodyAsString();
                Map<String, Object> body = parseBody(raw);
                log.warn(
                        "Admin Shiprocket {} HTTP {} orderId={} body={}",
                        action,
                        status,
                        orderId,
                        raw != null && raw.length() > 2000 ? raw.substring(0, 2000) : raw
                );
                if (status == 403) {
                    throw new IllegalStateException(
                            "user-service rejected internal key (HTTP 403). "
                                    + "Set the same INTERNAL_SERVICE_KEY on admin-service and user-service."
                    );
                }
                if (status == 404) {
                    throw new IllegalStateException(
                            "user-service Shiprocket endpoint not found (HTTP 404). "
                                    + "Redeploy/restart flintnthread.service (user-service)."
                    );
                }
                throw new IllegalStateException(
                        "user-service HTTP " + status + ": " + extractMessage(body, raw)
                );
            } catch (IllegalStateException e) {
                throw e;
            } catch (ResourceAccessException e) {
                last = e;
                String detail = rootMessage(e);
                log.warn(
                        "Admin Shiprocket {} connection issue orderId={} attempt={}: {}",
                        action,
                        orderId,
                        attempt,
                        detail
                );
                if (attempt < 2 && isRetryableConnection(detail)) {
                    try {
                        Thread.sleep(800L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                throw new IllegalStateException(
                        "Cannot reach user-service at " + base
                                + " (" + detail + "). On VPS set USER_SERVICE_URL=http://127.0.0.1:8080 "
                                + "and ensure flintnthread.service is running.",
                        e
                );
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Shiprocket " + action + " failed: " + rootMessage(e),
                        e
                );
            }
        }
        throw new IllegalStateException(
                "Shiprocket " + action + " failed: " + rootMessage(last),
                last
        );
    }

    private static String normalizeBase(String userServiceUrl) {
        String base = userServiceUrl == null ? "http://127.0.0.1:8080" : userServiceUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Prefer loopback for service-to-service on same VPS.
        if (base.contains("flintnthread.in") || base.contains("flintnthread.online")) {
            return "http://127.0.0.1:8080";
        }
        return base;
    }

    private static boolean isRetryableConnection(String detail) {
        if (detail == null) {
            return true;
        }
        String d = detail.toLowerCase();
        return d.contains("closedchannel")
                || d.contains("connection reset")
                || d.contains("connection refused")
                || d.contains("broken pipe")
                || d.contains("premature")
                || d.contains("eof");
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
        String fallback = e != null ? e.getClass().getSimpleName() : "Error";
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
