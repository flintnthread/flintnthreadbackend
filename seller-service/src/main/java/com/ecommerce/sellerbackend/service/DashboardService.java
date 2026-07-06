package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.DashboardChartsResponse;
import com.ecommerce.sellerbackend.dto.DashboardResponse;
import com.ecommerce.sellerbackend.dto.DashboardStatsByPeriodResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse getDashboard(Long sellerId);

    DashboardChartsResponse getCharts(Long sellerId, String period);

    DashboardChartsResponse getCharts(Long sellerId, LocalDate from, LocalDate to);

    DashboardStatsByPeriodResponse getStatsByPeriod(Long sellerId);
}
