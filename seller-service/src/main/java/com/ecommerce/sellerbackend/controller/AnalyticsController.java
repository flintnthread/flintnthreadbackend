package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.AnalyticsOverviewDto;
import com.ecommerce.sellerbackend.dto.AnalyticsSalesResponse;
import com.ecommerce.sellerbackend.dto.PaymentMethodBreakdownDto;
import com.ecommerce.sellerbackend.dto.SalesTrendPointDto;
import com.ecommerce.sellerbackend.dto.TopSellingProductResponse;
import com.ecommerce.sellerbackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seller/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final AnalyticsService analyticsService;

    @GetMapping("/sales")
    public AnalyticsSalesResponse sales(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "month") String period) {
        return analyticsService.getSales(requireSellerId(sellerId), period);
    }

    @GetMapping("/top-products")
    public List<TopSellingProductResponse> topProducts(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "20") int limit) {
        return analyticsService.getTopProducts(requireSellerId(sellerId), limit);
    }

    @GetMapping("/sales-trend")
    public List<SalesTrendPointDto> salesTrend(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long id = requireSellerId(sellerId);
        if (from != null && to != null) {
            return analyticsService.getSalesTrend(id, from, to);
        }
        return analyticsService.getSalesTrend(id, period);
    }

    @GetMapping("/orders-trend")
    public List<SalesTrendPointDto> ordersTrend(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "week") String period) {
        return analyticsService.getOrdersTrend(requireSellerId(sellerId), period);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodBreakdownDto> paymentMethods(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "month") String period) {
        return analyticsService.getPaymentMethods(requireSellerId(sellerId), period);
    }

    @GetMapping("/overview")
    public AnalyticsOverviewDto overview(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "All Channels") String channel) {
        return analyticsService.getOverview(requireSellerId(sellerId), period, channel);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
