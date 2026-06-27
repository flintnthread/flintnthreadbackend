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

        Integer orderId = parseInteger(firstNonBlank(eventData,
                "order_id", "orderId", "channel_order_id", "channelOrderId"));
        String shiprocketOrderId = firstNonBlank(eventData,
                "shiprocket_order_id", "shiprocketOrderId", "sr_order_id", "srOrderId");
        String shipmentId = firstNonBlank(eventData, "shipment_id", "shipmentId");
        String awbCode = firstNonBlank(eventData, "awb_code", "awb", "awbCode");
        String courierName = firstNonBlank(eventData, "courier_name", "courierName");
        String currentStatus = firstNonBlank(eventData, "current_status", "currentStatus", "status");
        String requestPayloadJson = toJson(payload);
        Long orderIdLong = orderId == null ? null : orderId.longValue();
        Optional<Order> orderOptional = orderIdLong == null ? Optional.empty() : orderRepository.findById(orderIdLong);
        Integer webhookOrderId = orderOptional.isPresent() ? orderId : null;

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
        log.info("Shiprocket webhook saved: orderId={}, awb={}, status={}", orderId, awbCode, currentStatus);

        if (orderId == null || isBlank(currentStatus)) {
            shiprocketSyncLogService.logSync(
                    webhookOrderId,
                    null,
                    shiprocketOrderId,
                    "WEBHOOK_RECEIVED",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    "Missing required order_id or current_status in webhook payload"
            );
            return;
        }

        if (orderOptional.isEmpty()) {
            log.warn("Shiprocket webhook references unknown order id={}", orderId);
            shiprocketSyncLogService.logSync(
                    webhookOrderId,
                    null,
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    "Order not found for id " + orderId
            );
            return;
        }

        Order order = orderOptional.get();
        String mappedOrderStatus = mapToOrderStatusForOrderTable(currentStatus);
        if (isBlank(mappedOrderStatus)) {
            shiprocketSyncLogService.logSync(
                    orderId,
                    order.getOrderNumber(),
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    "Unsupported Shiprocket status: " + currentStatus
            );
            return;
        }

        try {
            order.setOrderStatus(mappedOrderStatus);
            orderRepository.save(order);
        } catch (Exception e) {
            log.warn("Skipping order status update for orderId={} due to schema mismatch", orderId, e);
            shiprocketSyncLogService.logSync(
                    webhookOrderId,
                    order.getOrderNumber(),
                    shiprocketOrderId,
                    "ORDER_STATUS_SYNC",
                    "FAILED",
                    requestPayloadJson,
                    null,
                    "Failed to update order_status with mapped value: " + mappedOrderStatus
            );
            return;
        }

        OrderStatus orderStatusEnum = mapToOrderStatusEnum(mappedOrderStatus);
        if (orderStatusEnum != null) {
            try {
                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .status(orderStatusEnum)
                        .comment("Updated from Shiprocket webhook: " + currentStatus)
                        .build();
                orderStatusHistoryRepository.save(history);
            } catch (Exception e) {
                // Keep webhook processing successful even if status history schema is stricter.
                log.warn("Skipping order_status_history insert for orderId={} due to schema mismatch", orderId, e);
            }
        }

        shiprocketSyncLogService.logSync(
                orderId,
                order.getOrderNumber(),
                shiprocketOrderId,
                "ORDER_STATUS_SYNC",
                "SUCCESS",
                requestPayloadJson,
                "Mapped status=" + mappedOrderStatus,
                null
        );
    }

    private Map<String, Object> extractEventData(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) dataMap;
            return casted;
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
            if (!asText.isEmpty()) {
                return asText;
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String mapToOrderStatusForOrderTable(String sourceStatus) {
        if (isBlank(sourceStatus)) {
            return null;
        }
        String normalized = normalize(sourceStatus);
        switch (normalized) {

            case "new":
                return "new";

            case "confirmed":
                return "confirmed";

            case "processing":
                return "processing";

            case "packed":
                return "packed";

            case "awb_assigned":
                return "awb_assigned";

            case "pickup_scheduled":
                return "pickup_scheduled";

            case "picked_up":
                return "picked_up";

            case "in_transit":
            case "shipped":
                return "in_transit";

            case "out_for_delivery":
                return "out_for_delivery";

            case "delivered":
                return "delivered";

            case "cancelled":
            case "canceled":
                return "cancelled";

            case "rto_initiated":
                return "rto_initiated";

            case "rto_delivered":
                return "rto_delivered";

            case "return_initiated":
            case "returned":
                return "returned";

            default:
                return null;
        }
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
                 "in_transit"
                    -> OrderStatus.CONFIRMED;

            case "out_for_delivery"
                    -> OrderStatus.OUT_FOR_DELIVERY;

            case "delivered"
                    -> OrderStatus.DELIVERED;

            case "cancelled",
                 "rto_initiated",
                 "rto_delivered"
                    -> OrderStatus.CANCELLED;

            case "returned"
                    -> OrderStatus.RETURNED;

            default -> null;
        };
    }

    private String normalize(String status) {
        return status.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
