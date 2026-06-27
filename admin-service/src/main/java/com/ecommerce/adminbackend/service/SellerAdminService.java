package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.List;
import java.util.Map;

public interface SellerAdminService {

    PageResponse<Map<String, Object>> listSellers(String status, String search, int page, int size);

    PageResponse<Map<String, Object>> listAdminApprovedSellers(String search, int page, int size);

    Map<String, Object> adminApprovedLocationStats();

    PageResponse<Map<String, Object>> listSellersForGraph(String search, Long sellerId, int page, int size);

    List<Map<String, Object>> listSellerGraphNames();

    Map<String, Object> analyticsSummary(String filterType, Integer year, String fromDate, String toDate, Long sellerId);

    Map<String, Object> analyticsChart(String filterType, Integer year, String fromDate, String toDate, Long sellerId);

    List<Map<String, Object>> analyticsInsights(String filterType, Integer year, String fromDate, String toDate, Long sellerId);

    List<String> analyticsYearOptions();

    Map<String, Object> blockSeller(Long id);

    Map<String, Object> unblockSeller(Long id);

    PageResponse<Map<String, Object>> listPendingBank(int page, int size);

    Map<String, Object> bankStats();

    Map<String, Object> getBankDetails(Long id);

    Map<String, Object> approveBank(Long id, String note);

    Map<String, Object> rejectBank(Long id, String note);

    Map<String, Object> getSeller(Long id);

    PageResponse<Map<String, Object>> listShiprocketSellers(String status, int page, int size);

    PageResponse<Map<String, Object>> listBankVerifications(String status, int page, int size);
}
