package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.AnalyticsOverviewDto;
import com.ecommerce.sellerbackend.dto.AnalyticsSalesResponse;
import com.ecommerce.sellerbackend.dto.PaymentMethodBreakdownDto;
import com.ecommerce.sellerbackend.dto.SalesTrendPointDto;
import com.ecommerce.sellerbackend.dto.TopSellingProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {
    AnalyticsSalesResponse getSales(Long sellerId, String period);

    List<TopSellingProductResponse> getTopProducts(Long sellerId, int limit);

    List<SalesTrendPointDto> getSalesTrend(Long sellerId, String period);

    List<SalesTrendPointDto> getSalesTrend(Long sellerId, LocalDate from, LocalDate to);

    List<SalesTrendPointDto> getOrdersTrend(Long sellerId, String period);

    List<SalesTrendPointDto> getProductsTrend(Long sellerId, String period);

    List<PaymentMethodBreakdownDto> getPaymentMethods(Long sellerId, String period);

    AnalyticsOverviewDto getOverview(Long sellerId, String period, String channel);
}
