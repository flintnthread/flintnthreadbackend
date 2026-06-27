package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AnalyticsOverviewDto {
    private BigDecimal total;
    private long orders;
    private double aov;
    private long returns;
    private long cancels;
    private long replacements;
    private List<AnalyticsChannelDto> channels;
    private List<PaymentMethodBreakdownDto> paymentMethods;
}
