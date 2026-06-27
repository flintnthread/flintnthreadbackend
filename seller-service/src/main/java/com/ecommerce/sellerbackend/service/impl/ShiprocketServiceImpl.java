package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.dto.financial.ShiprocketTrackingEventDto;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.service.ShiprocketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShiprocketServiceImpl implements ShiprocketService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${shiprocket.api.base-url:https://apiv2.shiprocket.in}")
    private String baseUrl;

    @Value("${shiprocket.email:}")
    private String email;

    @Value("${shiprocket.password:}")
    private String password;

    @Override
    @Transactional
    public ShiprocketSyncResponse syncTracking(Order order) {
        List<ShiprocketTrackingEventDto> events = new ArrayList<>();
        String status = order.getShiprocketStatus();
        String trackingUrl = order.getShiprocketTrackingUrl();

        if (order.getShiprocketAwbCode() != null && !order.getShiprocketAwbCode().isBlank()) {
            try {
                String token = authenticate();
                JsonNode track = fetchTracking(token, order.getShiprocketAwbCode());
                if (track != null) {
                    status = textOrNull(track.path("tracking_data").path("shipment_status"));
                    if (status == null) {
                        status = textOrNull(track.path("tracking_data").path("track_status"));
                    }
                    trackingUrl = textOrNull(track.path("tracking_data").path("track_url"));
                    JsonNode activities = track.path("tracking_data").path("shipment_track_activities");
                    if (activities.isArray()) {
                        int idx = 0;
                        for (JsonNode activity : activities) {
                            String activityDate = textOrNull(activity.path("date"));
                            String activityStatus = textOrNull(activity.path("activity"));
                            String location = textOrNull(activity.path("location"));
                            events.add(ShiprocketTrackingEventDto.builder()
                                    .date(activityDate != null ? activityDate : "")
                                    .time("")
                                    .status(activityStatus != null ? activityStatus : "Update")
                                    .location(location != null ? location : "")
                                    .description(activityStatus != null ? activityStatus : "")
                                    .type(idx == 0 ? "active" : "done")
                                    .build());
                            idx++;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Keep stored values when live sync fails.
            }
        }

        if (events.isEmpty() && order.getShiprocketStatus() != null) {
            events.add(ShiprocketTrackingEventDto.builder()
                    .date(order.getShiprocketSyncedAt() != null
                            ? DISPLAY_DATE_TIME.format(order.getShiprocketSyncedAt())
                            : "")
                    .time("")
                    .status(order.getShiprocketStatus())
                    .location("")
                    .description("Latest known shipment status")
                    .type("active")
                    .build());
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        order.setShiprocketStatus(status);
        order.setShiprocketTrackingUrl(trackingUrl);
        order.setShiprocketSyncedAt(syncedAt);
        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            orderRepository.save(order);
        }

        return ShiprocketSyncResponse.builder()
                .shiprocketOrderId(order.getShiprocketOrderId())
                .shipmentId(order.getShiprocketShipmentId())
                .awb(order.getShiprocketAwbCode())
                .courier(order.getShiprocketCourierName())
                .status(status != null ? status : order.getOrderStatus())
                .trackingUrl(trackingUrl != null ? trackingUrl : "")
                .syncedAt(DISPLAY_DATE_TIME.format(syncedAt))
                .events(events)
                .build();
    }

    private String authenticate() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", password
        ));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/external/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(response.body());
        String token = textOrNull(node.path("token"));
        if (token == null) {
            throw new IllegalStateException("Shiprocket authentication failed.");
        }
        return token;
    }

    private JsonNode fetchTracking(String token, String awb) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/external/courier/track/awb/" + awb))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return null;
        }
        return objectMapper.readTree(response.body());
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
