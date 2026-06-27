package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderPayoutAmountDto {
    private String orderKey;
    private Long orderId;
    private BigDecimal amount;
    private boolean found;
}
