package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationPaymentVerifyRequest {
    @NotBlank(message = "Order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Signature is required")
    private String razorpaySignature;
}
