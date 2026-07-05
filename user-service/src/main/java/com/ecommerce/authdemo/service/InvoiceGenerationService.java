package com.ecommerce.authdemo.service;

/**
 * Runs invoice generation outside the order transaction so a duplicate invoice
 * number cannot roll back a successfully placed order.
 */
public interface InvoiceGenerationService {
    void createForOrder(Integer orderId);
}
