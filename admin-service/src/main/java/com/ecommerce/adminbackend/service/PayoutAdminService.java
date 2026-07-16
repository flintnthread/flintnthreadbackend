package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface PayoutAdminService {

    PageResponse<Map<String, Object>> listPayouts(String status, int page, int size);

    Map<String, Object> payoutStats();

    Map<String, Object> getPayoutDetail(Long id);

    Map<String, Object> generateInvoice(Long id);

    String exportPayoutsCsv(String status, Integer minReminderDays);

    Map<String, Object> markPaid(Long id, String transactionRef, String adminNote, String status);

    PageResponse<Map<String, Object>> listPayoutRequests(String status, int page, int size);

    Map<String, Object> closePayoutRequest(Long requestId, String paymentStatusText, Long adminId);

    Map<String, Object> payoutAlerts();
}
