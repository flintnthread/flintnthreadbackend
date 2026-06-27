package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.BankEditApprovalRequest;
import com.ecommerce.sellerbackend.dto.BankEditRequest;
import com.ecommerce.sellerbackend.dto.BankEditResponse;
import com.ecommerce.sellerbackend.dto.financial.DashboardPeriodStatsDto;
import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.entity.SellerPayoutRequest;

import java.util.List;
import java.util.Map;

public interface SellerFinancialService {

    Map<String, Object> getDashboard(Long sellerId);

    Map<String, Object> getCharts(Long sellerId, String period);

    Map<String, DashboardPeriodStatsDto> getStatsByPeriod(Long sellerId);

    Map<String, Object> getAnalyticsSales(Long sellerId, String period);

    List<Map<String, Object>> getTopProducts(Long sellerId, int limit);

    Map<String, Object> getAnalyticsOverview(Long sellerId, String period, String channel);

    List<Map<String, Object>> getSalesTrend(Long sellerId, String period, String from, String to);

    List<Map<String, Object>> getPaymentMethods(Long sellerId, String period);

    Map<String, Object> getEarnings(Long sellerId);

    List<Map<String, Object>> getEarningsPayouts(Long sellerId);

    Map<String, Object> lookupOrderPayout(Long sellerId, String orderKey);

    Map<String, Object> requestEarningsPayout(Long sellerId, Map<String, Object> body);

    Map<String, Object> getPayoutSummary(Long sellerId);

    Map<String, Object> getBankDetails(Long sellerId);

    Map<String, Object> updateBankDetails(Long sellerId, Map<String, String> body);

    String confirmBankDetails(Long sellerId, String note);

    SellerPayoutRequest submitPayoutRequest(Long sellerId, String orderId, String sellerNote);

    List<SellerPayoutRequest> listPayoutRequests(Long sellerId);

    String exportPayoutRequestsCsv(Long sellerId);

    ShiprocketSyncResponse syncShiprocket(Long sellerId, String orderKey);

    BankEditResponse submitBankEditRequest(Long sellerId, BankEditRequest request);

    List<BankEditResponse> listBankEditRequests(Long sellerId);

    BankEditResponse approveBankEditRequest(Long requestId, BankEditApprovalRequest approvalRequest, Long adminId);
}
