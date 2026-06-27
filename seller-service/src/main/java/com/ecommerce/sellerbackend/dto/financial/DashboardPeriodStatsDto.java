package com.ecommerce.sellerbackend.dto.financial;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPeriodStatsDto {
    private String period;
    private int orders;
    private double sales;
    private String salesFormatted;
    private int views;
    private double rating;
    private int returns;
    private int newCustomers;
    private double conversionRate;
}
