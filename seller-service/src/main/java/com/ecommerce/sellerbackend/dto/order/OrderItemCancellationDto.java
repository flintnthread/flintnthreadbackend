package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemCancellationDto {
    private Integer id;
    private Long orderId;
    private Integer orderItemId;
    private String productName;
    private String reason;
    private String status;
    private String statusLabel;
    private String adminComment;
    private String processedAt;
    private String createdAt;
}
