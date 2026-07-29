package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.ShiprocketSyncLog;
import com.ecommerce.adminbackend.repository.ShiprocketSyncLogRepository;
import com.ecommerce.adminbackend.service.ShiprocketSyncLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketSyncLogServiceImpl implements ShiprocketSyncLogService {

    private static final int MAX_JSON_LENGTH = 16_000;

    private final ShiprocketSyncLogRepository shiprocketSyncLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void logPush(
            Long orderId,
            String orderNumber,
            String shiprocketOrderId,
            String status,
            Map<String, Object> requestPayload,
            Map<String, Object> responsePayload,
            String errorMessage
    ) {
        if (orderId == null) {
            return;
        }
        try {
            ShiprocketSyncLog entry = ShiprocketSyncLog.builder()
                    .orderId(orderId.intValue())
                    .orderNumber(orderNumber)
                    .shiprocketOrderId(shiprocketOrderId)
                    .action("ORDER_PUSH")
                    .status(status)
                    .requestData(toJson(requestPayload))
                    .responseData(toJson(responsePayload))
                    .errorMessage(truncate(errorMessage, 4000))
                    .build();
            shiprocketSyncLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write shiprocket_sync_logs for orderId={}", orderId, e);
        }
    }

    @Override
    public List<Map<String, Object>> getLogsForOrder(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return shiprocketSyncLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId.intValue())
                .stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(ShiprocketSyncLog entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("orderId", entity.getOrderId());
        row.put("orderNumber", entity.getOrderNumber());
        row.put("shiprocketOrderId", entity.getShiprocketOrderId());
        row.put("action", entity.getAction());
        row.put("status", entity.getStatus());
        row.put("requestData", entity.getRequestData());
        row.put("responseData", entity.getResponseData());
        row.put("errorMessage", entity.getErrorMessage());
        row.put("createdAt", entity.getCreatedAt());
        return row;
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            return truncate(json, MAX_JSON_LENGTH);
        } catch (JsonProcessingException e) {
            return truncate(String.valueOf(payload), MAX_JSON_LENGTH);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }
}
