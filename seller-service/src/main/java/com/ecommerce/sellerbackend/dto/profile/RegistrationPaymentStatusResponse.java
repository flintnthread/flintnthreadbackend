package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrationPaymentStatusResponse {
    private final boolean paid;
    private final boolean subscriptionActive;
    private final boolean paymentPending;
    private final String orderId;
    private final String paymentId;
    private final String paidAt;
    private final String subscriptionExpiresAt;
    private final int amount;
    private final double registrationFee;
    private final double gstAmount;
    private final double totalAmount;
    private final String currency;
    private final boolean invoiceEmailSent;
}
