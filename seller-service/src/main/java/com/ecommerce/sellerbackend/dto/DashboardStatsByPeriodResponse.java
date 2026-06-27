package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsByPeriodResponse {
    private DashboardPeriodStatsDto day;
    private DashboardPeriodStatsDto week;
    private DashboardPeriodStatsDto month;
    private DashboardPeriodStatsDto year;
}
