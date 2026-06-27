package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.InvoiceRequest;
import com.ecommerce.authdemo.dto.InvoiceResponse;

import java.util.List;

public interface InvoiceService {
    InvoiceResponse create(InvoiceRequest request);

    List<InvoiceResponse> getByOrderId(Integer orderId);

    InvoiceResponse getByInvoiceNumber(String invoiceNumber);

    InvoiceResponse update(Integer id, InvoiceRequest request);

    void delete(Integer id);

    /** HTML document stored/generated on the server for this invoice id. */
    String getInvoiceHtml(Integer invoiceId);

    /** Public invoice page for QR scans (`order_id` + optional `seller_id`). */
    String getPublicViewHtml(
            Integer orderId,
            Long sellerId,
            Long productId,
            Integer lineIndex
    );
}
