package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentMethodBreakdownDto {
    private String label;
    private double value;
    private double pct;
    private long orders;
}
