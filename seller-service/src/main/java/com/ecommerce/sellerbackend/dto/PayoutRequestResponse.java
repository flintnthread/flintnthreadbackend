package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayoutRequestResponse {
    private String transactionId;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private String status;
    private String message;
}
