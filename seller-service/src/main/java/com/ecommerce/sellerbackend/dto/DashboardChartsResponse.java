package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardChartsResponse {
    private String period;
    private List<SalesTrendPointDto> salesPoints;
    private List<SalesTrendPointDto> ordersPoints;
    private List<SalesTrendPointDto> productsPoints;
    private BigDecimal totalSales;
    private long totalOrders;
    private long totalUnitsSold;
    private String totalSalesFormatted;
}
