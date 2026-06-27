package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketSyncLogDTO {
    private Integer id;
    private Integer orderId;
    private String orderNumber;
    private String shiprocketOrderId;
    private String action;
    private String status;
    private String requestData;
    private String responseData;
    private String errorMessage;
    private LocalDateTime createdAt;
}
