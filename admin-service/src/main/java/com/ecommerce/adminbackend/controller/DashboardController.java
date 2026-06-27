package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Logger log = LogFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return dashboardService.getStats();
    }

    @GetMapping("/revenue-chart")
    public Map<String, Object> revenueChart(@RequestParam(defaultValue = "monthly") String timeframe) {
        return dashboardService.getRevenueChart(timeframe);
    }

    @GetMapping("/top-products")
    public List<Map<String, Object>> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getTopProducts(limit);
    }

    @GetMapping("/top-sellers")
    public List<Map<String, Object>> topSellers(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getTopSellers(limit);
    }

    @GetMapping("/inventory-alerts")
    public List<Map<String, Object>> inventoryAlerts(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getInventoryAlerts(limit);
    }

    @GetMapping("/activity")
    public List<Map<String, Object>> activity(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getActivityFeed(limit);
    }

    @GetMapping("/payments")
    public Map<String, Object> payments() {
        return dashboardService.getPaymentsSummary();
    }

    @GetMapping("/customer-insights")
    public Map<String, Object> customerInsights() {
        return dashboardService.getCustomerInsights();
    }

    @GetMapping("/seller-insights")
    public Map<String, Object> sellerInsights() {
        return dashboardService.getSellerInsights();
    }

    @GetMapping("/traffic")
    public Map<String, Object> traffic() {
        return dashboardService.getWebsiteTraffic();
    }

    @GetMapping("/catalog-quality")
    public Map<String, Object> catalogQuality() {
        return dashboardService.getCatalogQuality();
    }
}
