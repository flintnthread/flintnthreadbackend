package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.Enum.OrderStatus;
import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.entity.OrderStatusHistory;
import com.ecommerce.authdemo.entity.ShiprocketWebhook;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.OrderStatusHistoryRepository;
import com.ecommerce.authdemo.repository.ShiprocketWebhookRepository;
import com.ecommerce.authdemo.service.ShiprocketSyncLogService;
import com.ecommerce.authdemo.service.ShiprocketWebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketWebhookServiceImpl implements ShiprocketWebhookService {

    private final ShiprocketWebhookRepository shiprocketWebhookRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ShiprocketSyncLogService shiprocketSyncLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleWebhook(Map<String, Object> payload) {
        Map<String, Object> eventData = extractEventData(payload);

        String channelOrderId = firstNonBlank(eventData,
                "channel_order_id", "channelOrderId", "order_number", "orderNumber");
        String shiprocketOrderId = firstNonBlank(eventData,
                "sr_order_id", "srOrderId", "shiprocket_order_id", "shiprocketOrderId",
                "order_id", "orderId");
        String shipmentId = firstNonBlank(eventData, "shipment_id", "shipmentId");
        String awbCode = firstNonBlank(eventData, "awb_code", "awb", "awbCode");
        String courierName = firstNonBlank(eventData, "courier_name", "courierName", "courier");
        String currentStatus = firstNonBlank(eventData,
                "current_status", "currentStatus", "shipment_status", "shipmentStatus", "status");
        String trackingUrl = firstNonBlank(eventData,
                "tracking_url", "trackingUrl", "track_url", "trackUrl");
        String requestPayloadJson = toJson(payload);

        Order order = resolveOrder(channelOrderId, shiprocketOrderId, awbCode);
        Integer webhookOrderId = order != null && order.getId() != null
                ? order.getId().intValue()
                : null;

        ShiprocketWebhook webhook = ShiprocketWebhook.builder()
                .orderId(webhookOrderId)
                .shiprocketOrderId(shiprocketOrderId)
                .shipmentId(shipmentId)
                .awbCode(awbCode)
                .courierName(courierName)
                .currentStatus(currentStatus)
                .webhookData(requestPayloadJson)
                .build();

        shiprocketWebhookRepository.save(webhook);
        log.info("Shiprocket webhook saved: orderId={}, awb={}, status={}",
                webhookOrderId, awbCode, currentStatus);

        if (order == null) {
            log.warn("Shiprocket webhook references unknown order channel={} sr={} awb={}",
                    channelOrderId, shiprocketOrderId, awbCode);
            shiprocketSyncLogService.logSync(
                    webhookOrderId,
                    null,
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    "Order not found for webhook identifiers"
            );
            return;
        }

        try {
            if (!isBlank(awbCode)) {
                order.setShiprocketAwbCode(awbCode.trim());
            }
            if (!isBlank(shiprocketOrderId)
                    && (isBlank(channelOrderId)
                    || !shiprocketOrderId.trim().equalsIgnoreCase(channelOrderId.trim()))) {
                order.setShiprocketOrderId(shiprocketOrderId.trim());
            }
            if (!isBlank(shipmentId)) {
                order.setShiprocketShipmentId(shipmentId.trim());
            }
            if (!isBlank(courierName)) {
                order.setShiprocketCourierName(courierName.trim());
            } else if (isBlank(order.getShiprocketCourierName())) {
                order.setShiprocketCourierName("Shiprocket");
            }

            String resolvedAwb = !isBlank(order.getShiprocketAwbCode())
                    ? order.getShiprocketAwbCode().trim()
                    : null;
            String resolvedTracking = !isBlank(trackingUrl) ? trackingUrl.trim() : null;
            if (isBlank(resolvedTracking) && !isBlank(resolvedAwb)) {
                resolvedTracking = "https://shiprocket.co/tracking/" + resolvedAwb;
            }
            if (!isBlank(resolvedTracking)) {
                order.setShiprocketTrackingUrl(resolvedTracking);
            }

            String mappedOrderStatus = mapToOrderStatusForOrderTable(currentStatus);
            if (!isBlank(mappedOrderStatus)) {
                order.setOrderStatus(mappedOrderStatus);
                order.setShiprocketStatus(mappedOrderStatus);
            } else if (!isBlank(resolvedAwb)) {
                order.setOrderStatus("awb_assigned");
                order.setShiprocketStatus("awb_assigned");
                mappedOrderStatus = "awb_assigned";
            }

            order.setShiprocketSyncedAt(LocalDateTime.now());
            orderRepository.save(order);

            OrderStatus orderStatusEnum = mapToOrderStatusEnum(
                    !isBlank(mappedOrderStatus) ? mappedOrderStatus : order.getOrderStatus()
            );
            if (orderStatusEnum != null) {
                try {
                    OrderStatusHistory history = OrderStatusHistory.builder()
                            .order(order)
                            .status(orderStatusEnum)
                            .comment("Updated from Shiprocket webhook: " + currentStatus)
                            .build();
                    orderStatusHistoryRepository.save(history);
                } catch (Exception e) {
                    log.warn("Skipping order_status_history insert for orderId={} due to schema mismatch",
                            order.getId(), e);
                }
            }

            shiprocketSyncLogService.logSync(
                    order.getId() != null ? order.getId().intValue() : null,
                    order.getOrderNumber(),
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "SUCCESS",
                    requestPayloadJson,
                    "AWB=" + order.getShiprocketAwbCode()
                            + " trackingUrl=" + order.getShiprocketTrackingUrl()
                            + " status=" + order.getOrderStatus(),
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to apply Shiprocket webhook to orderId={}", order.getId(), e);
            shiprocketSyncLogService.logSync(
                    webhookOrderId,
                    order.getOrderNumber(),
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    e.getMessage()
            );
        }
    }

    private Order resolveOrder(String channelOrderId, String shiprocketOrderId, String awb) {
        if (!isBlank(channelOrderId)) {
            Optional<Order> byNumber = orderRepository.findByOrderNumber(channelOrderId.trim());
            if (byNumber.isPresent()) {
                return byNumber.get();
            }
        }
        if (!isBlank(awb)) {
            Optional<Order> byAwb = orderRepository.findByShiprocketAwbCode(awb.trim());
            if (byAwb.isPresent()) {
                return byAwb.get();
            }
        }
        if (!isBlank(shiprocketOrderId)) {
            String srId = shiprocketOrderId.trim();
            Optional<Order> bySr = orderRepository.findByShiprocketOrderId(srId);
            if (bySr.isPresent()) {
                return bySr.get();
            }
            Optional<Order> byNumber = orderRepository.findByOrderNumber(srId);
            if (byNumber.isPresent()) {
                return byNumber.get();
            }
            if (srId.matches("^\\d+$")) {
                try {
                    return orderRepository.findById(Long.parseLong(srId)).orElse(null);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Map<String, Object> extractEventData(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Map<String, Object> merged = new HashMap<>(payload);
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) dataMap;
            merged.putAll(casted);
            return merged;
        }
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize webhook payload", e);
            return payload.toString();
        }
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            String asText = value.toString().trim();
            if (!asText.isEmpty() && !"null".equalsIgnoreCase(asText)) {
                return asText;
            }
        }
        return null;
    }

    private String mapToOrderStatusForOrderTable(String sourceStatus) {
        if (isBlank(sourceStatus)) {
            return null;
        }
        String normalized = normalize(sourceStatus);
        return switch (normalized) {
            case "new" -> "new";
            case "confirmed" -> "confirmed";
            case "processing" -> "processing";
            case "packed" -> "packed";
            case "awb_assigned", "awbassigned" -> "awb_assigned";
            case "pickup_scheduled", "pickup_generated", "pickup_queued" -> "pickup_scheduled";
            case "picked_up", "shipped" -> "picked_up";
            case "in_transit", "intransit" -> "in_transit";
            case "out_for_delivery", "ofd" -> "out_for_delivery";
            case "delivered" -> "delivered";
            case "cancelled", "canceled" -> "cancelled";
            case "rto_initiated", "rto_in_transit" -> "rto_initiated";
            case "rto_delivered" -> "rto_delivered";
            case "return_initiated", "returned" -> "returned";
            default -> null;
        };
    }

    private OrderStatus mapToOrderStatusEnum(String orderTableStatus) {
        String normalized = normalize(orderTableStatus);
        return switch (normalized) {
            case "new" -> OrderStatus.CREATED;
            case "confirmed",
                 "processing",
                 "packed",
                 "awb_assigned",
                 "pickup_scheduled",
                 "picked_up",
                 "in_transit" -> OrderStatus.CONFIRMED;
            case "out_for_delivery" -> OrderStatus.OUT_FOR_DELIVERY;
            case "delivered" -> OrderStatus.DELIVERED;
            case "cancelled", "rto_initiated", "rto_delivered" -> OrderStatus.CANCELLED;
            case "returned" -> OrderStatus.RETURNED;
            default -> null;
        };
    }

    private String normalize(String status) {
        if (status == null) {
            return "";
        }
        return status.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
