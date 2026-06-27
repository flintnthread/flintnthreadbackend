package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrationPaymentStatusResponse {
    private final boolean paid;
    private final String orderId;
    private final String paymentId;
    private final String paidAt;
    private final int amount;
    private final String currency;
}
