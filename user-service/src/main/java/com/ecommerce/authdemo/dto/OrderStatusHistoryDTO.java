package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusHistoryDTO {
    private String status;
    private String comment;
    private String createdAt;
}
