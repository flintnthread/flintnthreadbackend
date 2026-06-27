package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderReplacementDto {
    private Integer id;
    private Long orderId;
    private Integer orderItemId;
    private String productName;
    private String reason;
    private String description;
    private String status;
    private String statusLabel;
    private String adminComment;
    private String shiprocketReturnId;
    private String trackingNumber;
    private String shippingProvider;
    private String processedAt;
    private String createdAt;
}
