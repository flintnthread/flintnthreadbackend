package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusHistoryEntryDto {
    private Long id;
    private Long orderId;
    private String status;
    private String statusLabel;
    private String comment;
    private Long createdBy;
    private String createdAt;
}
