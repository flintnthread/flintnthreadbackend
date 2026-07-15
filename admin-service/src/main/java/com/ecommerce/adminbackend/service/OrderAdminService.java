package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface OrderAdminService {

    PageResponse<Map<String, Object>> listOrders(
            String status,
            String paymentStatus,
            String paymentMethod,
            String search,
            String sort,
            Long sellerId,
            int page,
            int size);

    Map<String, Object> getOrderStats();

    Map<String, Object> getOrder(Long id);

    Map<String, Object> updateGstStatus(Long id, String gstStatus);

    Map<String, Object> updateOrderStatus(Long id, String status, String comment, Long adminUserId, boolean notifyCustomer);

    Map<String, Object> generateInvoice(Long id);

    byte[] generateInvoicePdf(Long id);

    Map<String, Object> generateShippingLabel(Long id);

    byte[] generateShippingLabelPdf(Long id);

    String exportOrdersCsv(
            String status,
            String paymentStatus,
            String paymentMethod,
            String search,
            String sort);
}
