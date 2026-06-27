package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderExchangeDto {
    private Integer id;
    private Long orderId;
    private Integer orderItemId;
    private String productName;
    private String reason;
    private String description;
    private Integer exchangeColor;
    private Integer exchangeSize;
    private String status;
    private String statusLabel;
    private String adminComment;
    private String shiprocketOrderId;
    private String shiprocketShipmentId;
    private String shiprocketAwbCode;
    private String trackingNumber;
    private String shippingProvider;
    private String processedAt;
    private String createdAt;
}
