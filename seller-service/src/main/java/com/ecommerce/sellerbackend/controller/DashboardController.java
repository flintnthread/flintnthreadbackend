package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.DashboardChartsResponse;
import com.ecommerce.sellerbackend.dto.DashboardResponse;
import com.ecommerce.sellerbackend.dto.DashboardStatsByPeriodResponse;
import com.ecommerce.sellerbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/seller/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse get(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return dashboardService.getDashboard(requireSellerId(sellerId));
    }

    @GetMapping("/charts")
    public DashboardChartsResponse charts(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long id = requireSellerId(sellerId);
        if (from != null && to != null) {
            return dashboardService.getCharts(id, from, to);
        }
        return dashboardService.getCharts(id, period);
    }

    @GetMapping("/stats-by-period")
    public DashboardStatsByPeriodResponse statsByPeriod(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return dashboardService.getStatsByPeriod(requireSellerId(sellerId));
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
