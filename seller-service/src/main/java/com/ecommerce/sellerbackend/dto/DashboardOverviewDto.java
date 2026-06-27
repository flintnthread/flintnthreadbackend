package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardOverviewDto {
    private int orders;
    private BigDecimal sales;
    private long views;
    private double rating;
    private long reviewCount;
    private String salesFormatted;
}
