package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AnalyticsSalesResponse {
    private String period;
    private BigDecimal totalSales;
    private long totalOrders;
    private String salesFormatted;
    private List<AnalyticsChannelDto> channels;
}
