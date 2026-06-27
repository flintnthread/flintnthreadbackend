package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ShiprocketSyncLogDTO;
import com.ecommerce.authdemo.entity.ShiprocketSyncLog;
import com.ecommerce.authdemo.repository.ShiprocketSyncLogRepository;
import com.ecommerce.authdemo.service.ShiprocketSyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketSyncLogServiceImpl implements ShiprocketSyncLogService {

    private final ShiprocketSyncLogRepository shiprocketSyncLogRepository;

    @Override
    public void logSync(Integer orderId,
                        String orderNumber,
                        String shiprocketOrderId,
                        String action,
                        String status,
                        String requestData,
                        String responseData,
                        String errorMessage) {
        if (orderId == null) {
            return;
        }

        try {
            ShiprocketSyncLog logEntry = ShiprocketSyncLog.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .shiprocketOrderId(shiprocketOrderId)
                    .action(action)
                    .status(status)
                    .requestData(requestData)
                    .responseData(responseData)
                    .errorMessage(errorMessage)
                    .build();

            shiprocketSyncLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to write shiprocket_sync_logs for orderId={}", orderId, e);
        }
    }

    @Override
    public List<ShiprocketSyncLogDTO> getSyncLogs(Integer orderId,
                                                  String orderNumber,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        String normalizedOrderNumber = normalize(orderNumber);

        return shiprocketSyncLogRepository
                .findWithFilters(orderId, normalizedOrderNumber, fromDateTime, toDateTime)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ShiprocketSyncLogDTO toDTO(ShiprocketSyncLog entity) {
        return ShiprocketSyncLogDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .orderNumber(entity.getOrderNumber())
                .shiprocketOrderId(entity.getShiprocketOrderId())
                .action(entity.getAction())
                .status(entity.getStatus())
                .requestData(entity.getRequestData())
                .responseData(entity.getResponseData())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
