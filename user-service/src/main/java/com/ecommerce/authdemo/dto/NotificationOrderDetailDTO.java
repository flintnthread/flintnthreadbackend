package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NotificationOrderDetailDTO {
    private Long orderId;
    private String orderNumber;
    /** IST formatted order placed time from backend. */
    private String orderDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String deliveryAddress;
    private String orderStatus;
}
