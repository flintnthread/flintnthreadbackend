package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.ShiprocketSyncLog;

import java.util.List;
import java.util.Map;

public interface ShiprocketSyncLogService {

    void logPush(
            Long orderId,
            String orderNumber,
            String shiprocketOrderId,
            String status,
            Map<String, Object> requestPayload,
            Map<String, Object> responsePayload,
            String errorMessage
    );

    List<Map<String, Object>> getLogsForOrder(Long orderId);
}
