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

    /** Seller account createdAt (join date). */
    private final String joinedAt;
    /** When the 3-month (configurable) unpaid grace ends. */
    private final String graceEndsAt;
    /** True when seller has never paid and is still within grace. */
    private final boolean graceActive;
    private final long daysRemainingInGrace;
    /** True when never paid and grace has ended — first annual fee is due. */
    private final boolean firstPaymentDue;
}
