package com.ecommerce.sellerbackend.dto.financial;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOverviewDto {
    private int orders;
    private double sales;
    private int views;
    private double rating;
    private int reviewCount;
    private String salesFormatted;
}
