package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOrderSummaryDto {
    private int pending;
    private int processing;
    private int shipped;
    private int delivered;
    private int returns;
}
