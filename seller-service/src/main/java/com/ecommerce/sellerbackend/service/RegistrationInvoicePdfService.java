package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.entity.Seller;

public interface RegistrationInvoicePdfService {
    byte[] generateRegistrationInvoice(
            Seller seller,
            String invoiceNumber,
            String paymentId,
            String orderId,
            int amountInPaise,
            String paidAtText
    );
}
