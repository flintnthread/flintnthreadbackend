package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletAmountRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
    private Double amount;

    /** When set, debits wallet and links the transaction to this order. */
    private Long orderId;

    private String orderNumber;
}
