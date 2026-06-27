package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VariantPricingPreviewRequest {
    private BigDecimal mrpExcl;
    private BigDecimal sellingExcl;
    private BigDecimal weightKg;
    private Integer categorySubId;
    private Integer subcategoryId;
    private BigDecimal discountOverride;
    private BigDecimal gstPercent;
}
