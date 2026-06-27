package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VariantPricingPreviewResponse {
    private BigDecimal mrpExcl;
    private BigDecimal sellingExcl;
    private BigDecimal gstPercent;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalPrice;
    private BigDecimal mrpInclGst;
    private BigDecimal commissionPercent;
    private BigDecimal commissionAmount;
    private BigDecimal intraCityCharge;
    private BigDecimal metroMetroCharge;
    private BigDecimal totalIntraCity;
    private BigDecimal totalMetroMetro;
    private String weightSlabLabel;
    private Boolean deliveryCustom;
}
