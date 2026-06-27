package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrationPaymentOrderResponse {
    private final String keyId;
    private final String orderId;
    private final int amount;
   private final double registrationFee;
    private final double gstAmount;
    private final double totalAmount;
    private final String currency;
    private final String receipt;
    private final boolean paid;
}
