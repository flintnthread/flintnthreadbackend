package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.dto.financial.ShiprocketTrackingEventDto;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.entity.OrderItem;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.service.PlatformIntegrationSettings;
import com.ecommerce.sellerbackend.service.ShiprocketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ShiprocketServiceImpl implements ShiprocketService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
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
        Exception lastError = null;

        try {
            String token = authenticate();
            JsonNode track = null;
            if (awb != null && !awb.isBlank()) {
                track = fetchTracking(token, "/courier/track/awb/" + awb.trim());
            }
            if (track == null && shipmentId != null && shipmentId.trim().matches("^\\d+$")) {
                track = fetchTracking(token, "/courier/track/shipment/" + shipmentId.trim());
            }
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
        } catch (Exception ex) {
            lastError = ex;
            log.warn("Shiprocket live sync failed orderId={} awb={} msg={}",
                    order.getId(), awb, ex.getMessage());
        }

        // Normalize numeric / mixed Shiprocket statuses into readable labels before save.
        String normalizedShiprocketStatus = normalizeShiprocketStatusLabel(status, awb);
        if (normalizedShiprocketStatus != null) {
            status = normalizedShiprocketStatus;
        }

        if (events.isEmpty() && status != null) {
            events.add(ShiprocketTrackingEventDto.builder()
                    .date(order.getShiprocketSyncedAt() != null
                            ? DISPLAY_DATE_TIME.format(order.getShiprocketSyncedAt())
                            : "")
                    .time("")
                    .status(status)
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

        // Must be values allowed by orders.order_status MySQL ENUM.
        String mappedOrderStatus = mapShiprocketToOrderStatus(status, awb);
        if (mappedOrderStatus != null && !mappedOrderStatus.isBlank()) {
            order.setOrderStatus(mappedOrderStatus);
            syncOrderItemsStatus(order.getId(), mappedOrderStatus);
        }

        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            try {
                orderRepository.save(order);
            } catch (Exception ex) {
                log.error("Failed saving Shiprocket sync for orderId={} status={} mapped={} msg={}",
                        order.getId(), status, mappedOrderStatus, ex.getMessage());
                throw ex;
            }
        } else if (lastError != null) {
            throw new IllegalStateException(
                    "Shiprocket sync failed and order row missing: " + lastError.getMessage(),
                    lastError);
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

    private void syncOrderItemsStatus(Long orderId, String mappedOrderStatus) {
        if (orderId == null || mappedOrderStatus == null || mappedOrderStatus.isBlank()) {
            return;
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            item.setStatus(mappedOrderStatus);
        }
        orderItemRepository.saveAll(items);
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

        String resolvedStatus = null;
        JsonNode shipmentTrack = firstNode(
                trackingData.path("shipment_track"),
                trackingData.path("shipmentTrack")
        );
        if (shipmentTrack != null && shipmentTrack.isArray() && shipmentTrack.size() > 0) {
            JsonNode first = shipmentTrack.get(0);
            resolvedStatus = firstText(
                    first.path("current_status"),
                    first.path("current_status_code"),
                    first.path("status")
            );
            if (resolvedStatus == null) {
                resolvedStatus = textOrNull(first.path("shipment_status"));
            }
        }

        if (resolvedStatus == null) {
            resolvedStatus = firstText(
                    trackingData.path("shipment_status"),
                    trackingData.path("track_status"),
                    trackingData.path("status"),
                    trackingData.path("current_status")
            );
        }

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
        if (resolvedAwb == null && shipmentTrack != null && shipmentTrack.isArray() && shipmentTrack.size() > 0) {
            resolvedAwb = firstText(
                    shipmentTrack.get(0).path("awb_code"),
                    shipmentTrack.get(0).path("awb")
            );
        }
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
        if (resolvedCourierName == null && shipmentTrack != null && shipmentTrack.isArray() && shipmentTrack.size() > 0) {
            resolvedCourierName = firstText(
                    shipmentTrack.get(0).path("courier_name"),
                    shipmentTrack.get(0).path("sr_courier_name")
            );
        }

        List<TrackingActivity> activities = new ArrayList<>();
        JsonNode activitiesNode = firstNode(
                trackingData.path("shipment_track_activities"),
                trackingData.path("activities"),
                trackingData.path("events")
        );
        if (activitiesNode != null && activitiesNode.isArray()) {
            for (JsonNode activity : activitiesNode) {
                String activityDate = textOrNull(activity.path("date"));
                String activityStatus = textOrNull(activity.path("activity"));
                String location = textOrNull(activity.path("location"));
                activities.add(new TrackingActivity(
                        activityDate,
                        activityStatus != null ? activityStatus : "Update",
                        location));
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

    private JsonNode fetchTracking(String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.warn("Shiprocket track GET {} failed status={}", path, response.statusCode());
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
        String text = node.isNumber() ? node.asText() : node.asText();
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text.trim();
    }

    public record TrackingSnapshot(String status, String trackingUrl, String awb,
                                   String shiprocketOrderId, String shipmentId,
                                   String courierName, List<TrackingActivity> activities) {
    }

    public record TrackingActivity(String date, String status, String location) {
    }

    /**
     * Convert Shiprocket numeric codes / free-text into a readable status label.
     */
    public static String normalizeShiprocketStatusLabel(String shiprocketStatus, String awb) {
        if (shiprocketStatus == null || shiprocketStatus.isBlank()) {
            return (awb != null && !awb.isBlank()) ? "AWB Assigned" : null;
        }
        String raw = shiprocketStatus.trim();
        if (raw.matches("^\\d+$")) {
            return switch (raw) {
                case "1" -> "AWB Assigned";
                case "2" -> "Label Generated";
                case "3" -> "Pickup Scheduled";
                case "4" -> "Pickup Queued";
                case "5", "8", "16", "45" -> "Cancelled";
                case "6", "18", "19", "20", "38", "39", "40", "41", "42", "43" -> "In Transit";
                case "7", "23", "26" -> "Delivered";
                case "9", "10", "14", "46" -> "RTO";
                case "12" -> "Lost";
                case "13", "21", "22" -> "Undelivered";
                case "15" -> "Pickup Rescheduled";
                case "17" -> "Out For Delivery";
                case "24", "25" -> "Damaged";
                default -> "In Transit";
            };
        }
        return raw;
    }

    /**
     * Map Shiprocket status into values allowed by orders.order_status ENUM:
     * pending, sent_to_seller, processing, completed, cancelled, refunded, returned,
     * replacement, awaiting_processing, awaiting_payment, shipped, delivered
     */
    public static String mapShiprocketToOrderStatus(String shiprocketStatus, String awb) {
        String label = normalizeShiprocketStatusLabel(shiprocketStatus, awb);
        if (label == null || label.isBlank()) {
            return null;
        }
        String s = label.trim().toLowerCase(Locale.ENGLISH)
                .replace("-", "_")
                .replace(" ", "_");

        if (s.contains("deliver")) return "delivered";
        if (s.contains("rto") || s.contains("return")) return "returned";
        if (s.contains("cancel")) return "cancelled";
        if (s.contains("out_for_delivery") || s.contains("ofd")) return "shipped";
        if (s.contains("in_transit") || s.contains("shipped") || s.contains("picked")
                || s.contains("pickup") || s.contains("undelivered") || s.contains("lost")
                || s.contains("damaged") || s.contains("flight") || s.contains("warehouse")
                || s.contains("courier") || s.contains("connection") || s.contains("reached")) {
            return "shipped";
        }
        if (s.contains("awb") || s.contains("label") || s.contains("process")
                || s.contains("pack") || s.contains("confirm") || s.contains("new")) {
            return "processing";
        }
        return (awb != null && !awb.isBlank()) ? "processing" : null;
    }
}
