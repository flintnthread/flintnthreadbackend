package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DeliveryWeightSlabResponse {
    private Long id;
    private String label;
    private BigDecimal minWeightKg;
    private BigDecimal maxWeightKg;
    private BigDecimal intraCityCharge;
    private BigDecimal metroMetroCharge;
    private Boolean custom;
}
