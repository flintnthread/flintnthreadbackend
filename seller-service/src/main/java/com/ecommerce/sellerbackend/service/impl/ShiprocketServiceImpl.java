package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.dto.financial.ShiprocketTrackingEventDto;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.service.PlatformIntegrationSettings;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ShiprocketServiceImpl implements ShiprocketService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final PlatformIntegrationSettings integrationSettings;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${shiprocket.api.base-url:https://apiv2.shiprocket.in/v1/external}")
    private String baseUrl;

    @Override
    @Transactional
    public ShiprocketSyncResponse syncTracking(Order order) {
        List<ShiprocketTrackingEventDto> events = new ArrayList<>();
        String status = order.getShiprocketStatus();
        String trackingUrl = order.getShiprocketTrackingUrl();
        String awb = order.getShiprocketAwbCode();
        String shiprocketOrderId = order.getShiprocketOrderId();
        String shipmentId = order.getShiprocketShipmentId();
        String courierName = order.getShiprocketCourierName();

        if (order.getShiprocketAwbCode() != null && !order.getShiprocketAwbCode().isBlank()) {
            try {
                String token = authenticate();
                JsonNode track = fetchTracking(token, order.getShiprocketAwbCode());
                if (track != null) {
                    TrackingSnapshot snapshot = parseTrackingSnapshot(track);
                    if (snapshot != null) {
                        if (snapshot.status() != null) {
                            status = snapshot.status();
                        }
                        if (snapshot.trackingUrl() != null) {
                            trackingUrl = snapshot.trackingUrl();
                        }
                        if (snapshot.awb() != null) {
                            awb = snapshot.awb();
                        }
                        if (snapshot.shiprocketOrderId() != null) {
                            shiprocketOrderId = snapshot.shiprocketOrderId();
                        }
                        if (snapshot.shipmentId() != null) {
                            shipmentId = snapshot.shipmentId();
                        }
                        if (snapshot.courierName() != null) {
                            courierName = snapshot.courierName();
                        }
                        for (int idx = 0; idx < snapshot.activities().size(); idx++) {
                            TrackingActivity activity = snapshot.activities().get(idx);
                            events.add(ShiprocketTrackingEventDto.builder()
                                    .date(activity.date() != null ? activity.date() : "")
                                    .time("")
                                    .status(activity.status() != null ? activity.status() : "Update")
                                    .location(activity.location() != null ? activity.location() : "")
                                    .description(activity.status() != null ? activity.status() : "")
                                    .type(idx == 0 ? "active" : "done")
                                    .build());
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
        order.setShiprocketOrderId(shiprocketOrderId);
        order.setShiprocketShipmentId(shipmentId);
        order.setShiprocketAwbCode(awb);
        order.setShiprocketCourierName(courierName);
        order.setShiprocketStatus(status);
        order.setShiprocketTrackingUrl(trackingUrl);
        order.setShiprocketSyncedAt(syncedAt);
        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            orderRepository.save(order);
        }

        return ShiprocketSyncResponse.builder()
                .shiprocketOrderId(shiprocketOrderId)
                .shipmentId(shipmentId)
                .awb(awb)
                .courier(courierName)
                .status(status != null ? status : order.getOrderStatus())
                .trackingUrl(trackingUrl != null ? trackingUrl : "")
                .syncedAt(DISPLAY_DATE_TIME.format(syncedAt))
                .events(events)
                .build();
    }

    public static TrackingSnapshot parseTrackingSnapshot(JsonNode response) {
        JsonNode trackingData = firstNode(
                response.path("tracking_data"),
                response.path("data").path("tracking_data"),
                response.path("data").path("tracking"),
                response.path("tracking")
        );
        if (trackingData == null || trackingData.isMissingNode() || trackingData.isNull()) {
            return null;
        }

        String resolvedStatus = firstText(
                trackingData.path("shipment_status"),
                trackingData.path("track_status"),
                trackingData.path("status")
        );
        String resolvedTrackingUrl = firstText(
                trackingData.path("track_url"),
                trackingData.path("tracking_url"),
                trackingData.path("url")
        );
        String resolvedAwb = firstText(
                trackingData.path("awb"),
                trackingData.path("awb_code"),
                trackingData.path("tracking_number"),
                response.path("awb"),
                response.path("awb_code"),
                response.path("data").path("awb")
        );
        String resolvedShiprocketOrderId = firstText(
                response.path("order_id"),
                response.path("data").path("order_id"),
                response.path("shiprocket_order_id"),
                response.path("data").path("shiprocket_order_id"),
                trackingData.path("order_id")
        );
        String resolvedShipmentId = firstText(
                response.path("shipment_id"),
                response.path("data").path("shipment_id"),
                response.path("shipment_ids").path(0),
                trackingData.path("shipment_id")
        );
        String resolvedCourierName = firstText(
                trackingData.path("courier_name"),
                trackingData.path("courier"),
                response.path("courier_name"),
                response.path("courier")
        );

        List<TrackingActivity> activities = new ArrayList<>();
        JsonNode activitiesNode = firstNode(
                trackingData.path("shipment_track_activities"),
                trackingData.path("activities"),
                trackingData.path("events")
        );
        if (activitiesNode != null && activitiesNode.isArray()) {
            int idx = 0;
            for (JsonNode activity : activitiesNode) {
                String activityDate = textOrNull(activity.path("date"));
                String activityStatus = textOrNull(activity.path("activity"));
                String location = textOrNull(activity.path("location"));
                activities.add(new TrackingActivity(
                        activityDate,
                        activityStatus != null ? activityStatus : "Update",
                        location));
                idx++;
            }
        }

        return new TrackingSnapshot(resolvedStatus, resolvedTrackingUrl, resolvedAwb,
                resolvedShiprocketOrderId, resolvedShipmentId, resolvedCourierName, activities);
    }

    private String authenticate() throws Exception {
        String email = integrationSettings.getShiprocketEmail();
        String password = integrationSettings.getShiprocketPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Shiprocket credentials missing. Set them in Admin → Platform Settings.");
        }
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email.trim(),
                "password", password
        ));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(response.body());
        String token = firstText(
                node.path("token"),
                node.path("data").path("token"),
                node.path("access_token"),
                node.path("data").path("accessToken")
        );
        if (token == null) {
            throw new IllegalStateException("Shiprocket authentication failed.");
        }
        return token;
    }

    private JsonNode fetchTracking(String token, String awb) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/courier/track/awb/" + awb))
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

    private static JsonNode firstNode(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String text = textOrNull(node);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    public record TrackingSnapshot(String status, String trackingUrl, String awb,
                                   String shiprocketOrderId, String shipmentId,
                                   String courierName, List<TrackingActivity> activities) {
    }

    public record TrackingActivity(String date, String status, String location) {
    }
}
