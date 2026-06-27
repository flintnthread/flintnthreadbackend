package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ShiprocketSyncLogDTO;

import java.time.LocalDate;
import java.util.List;

public interface ShiprocketSyncLogService {
    void logSync(Integer orderId,
                 String orderNumber,
                 String shiprocketOrderId,
                 String action,
                 String status,
                 String requestData,
                 String responseData,
                 String errorMessage);

    List<ShiprocketSyncLogDTO> getSyncLogs(Integer orderId,
                                           String orderNumber,
                                           LocalDate fromDate,
                                           LocalDate toDate);
}
