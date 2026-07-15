package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class WalletRechargeVerifyRequest {
    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String paymentId;

    @NotBlank
    private String signature;

    @NotNull
    @DecimalMin(value = "1.0")
    private Double amount;
}
