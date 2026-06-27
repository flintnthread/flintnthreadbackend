package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardPeriodStatsDto {
    private String period;
    private long orders;
    private BigDecimal sales;
    private String salesFormatted;
    private long views;
    private double rating;
    private long returns;
}
