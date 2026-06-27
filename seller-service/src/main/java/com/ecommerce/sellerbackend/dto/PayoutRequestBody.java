package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayoutRequestBody {

    @NotNull
    @DecimalMin(value = "1.0", message = "Minimum payout amount is ₹1")
    private BigDecimal amount;

    private String orderId;

    private String otp;

    private String description;
}
