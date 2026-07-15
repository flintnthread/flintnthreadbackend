package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletRechargeCreateRequest {
    @NotNull
    @DecimalMin(value = "1.0", message = "Recharge amount must be at least ₹1")
    private Double amount;
}
