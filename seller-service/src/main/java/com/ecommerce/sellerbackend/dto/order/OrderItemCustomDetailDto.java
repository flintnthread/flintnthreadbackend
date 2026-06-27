package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemCustomDetailDto {
    private Integer id;
    private Integer orderItemId;
    private String fieldKey;
    private String fieldLabel;
    private String valueText;
    private String valueFile;
    private String createdAt;
}
