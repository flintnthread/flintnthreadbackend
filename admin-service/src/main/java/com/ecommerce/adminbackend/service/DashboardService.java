package com.ecommerce.adminbackend.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getStats();

    Map<String, Object> getRevenueChart(String timeframe);

    List<Map<String, Object>> getTopProducts(int limit);

    List<Map<String, Object>> getTopSellers(int limit);

    List<Map<String, Object>> getInventoryAlerts(int limit);

    List<Map<String, Object>> getActivityFeed(int limit);

    Map<String, Object> getPaymentsSummary();

    Map<String, Object> getCustomerInsights();

    Map<String, Object> getSellerInsights();

    Map<String, Object> getWebsiteTraffic();

    Map<String, Object> getCatalogQuality();
}
