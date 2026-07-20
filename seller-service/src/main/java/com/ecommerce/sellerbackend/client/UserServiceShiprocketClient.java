package com.ecommerce.sellerbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls user-service to create Shiprocket shipment after seller confirms an order.
 */
@Component
@Slf4j
public class UserServiceShiprocketClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${app.user-service-url:http://127.0.0.1:8080}")
    private String userServiceUrl;

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    public void pushOrderAsync(Long orderId) {
        if (orderId == null || orderId <= 0) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                pushOrder(orderId);
            } catch (Exception e) {
                log.error("Shiprocket push failed for orderId={}: {}", orderId, e.getMessage(), e);
            }
        }, "seller-shiprocket-push-" + orderId);
        t.setDaemon(true);
        t.start();
    }

    public void pushOrder(Long orderId) throws Exception {
        if (internalServiceKey == null || internalServiceKey.isBlank()) {
            log.warn("app.internal-service-key not set — skipping Shiprocket push for orderId={}", orderId);
            return;
        }
        String base = userServiceUrl == null ? "http://127.0.0.1:8080" : userServiceUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/internal/shiprocket/orders/" + orderId + "/push";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("X-Internal-Service-Key", internalServiceKey)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "User-service Shiprocket push HTTP " + response.statusCode() + ": " + response.body()
            );
        }
        log.info("Shiprocket push OK for orderId={} response={}", orderId, response.body());
    }
}
