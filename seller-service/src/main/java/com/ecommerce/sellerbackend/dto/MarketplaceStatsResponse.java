package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketplaceStatsResponse {
    private final long sellersCount;
    private final long productsCount;
    private final long customersCount;
    private final int avgApprovalHours;
    private final String sellersDisplay;
    private final String productsDisplay;
    private final String customersDisplay;
    private final String approvalDisplay;
}
