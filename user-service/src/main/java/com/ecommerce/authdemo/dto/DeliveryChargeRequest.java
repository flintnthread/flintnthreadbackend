package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryChargeRequest {

    @NotBlank(message = "Weight slab is required")
    @Size(max = 50, message = "Weight slab must not exceed 50 characters")
    private String weightSlab;

    @NotNull(message = "Weight min is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Weight min must be >= 0")
    private BigDecimal weightMin;

    @NotNull(message = "Weight max is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Weight max must be >= 0")
    private BigDecimal weightMax;

    @NotNull(message = "Intra city charge is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Intra city charge must be >= 0")
    private BigDecimal intraCityCharge;

    @NotNull(message = "Metro to metro charge is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Metro to metro charge must be >= 0")
    private BigDecimal metroMetroCharge;

    private Boolean isCustom;
    private Boolean status;
}
