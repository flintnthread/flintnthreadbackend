package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.DashboardChartsResponse;
import com.ecommerce.sellerbackend.dto.DashboardResponse;
import com.ecommerce.sellerbackend.dto.DashboardStatsByPeriodResponse;

public interface DashboardService {
    DashboardResponse getDashboard(Long sellerId);

    DashboardChartsResponse getCharts(Long sellerId, String period);

    DashboardStatsByPeriodResponse getStatsByPeriod(Long sellerId);
}
