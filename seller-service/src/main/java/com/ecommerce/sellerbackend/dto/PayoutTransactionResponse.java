package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayoutTransactionResponse {
    private String id;
    private String orderId;
    private BigDecimal amount;
    private String date;
    private String status;
    private String type;
}
