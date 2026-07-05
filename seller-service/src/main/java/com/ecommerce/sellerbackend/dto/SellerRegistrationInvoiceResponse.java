package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerRegistrationInvoiceResponse {
    private final Long id;
    private final String invoiceNumber;
    private final String displayOrderNumber;
    private final String paymentId;
    private final int amount;
    private final double registrationFee;
    private final double gstAmount;
    private final double totalAmount;
    private final String currency;
    private final String paidAt;
}
