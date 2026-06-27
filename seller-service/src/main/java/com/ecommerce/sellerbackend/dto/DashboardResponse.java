package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponse {
    private DashboardOverviewDto overview;
    private DashboardOrderSummaryDto orderSummary;
    private List<DashboardTopProductDto> topProducts;
    private int totalProducts;
    private DashboardReferralDto referral;
}
