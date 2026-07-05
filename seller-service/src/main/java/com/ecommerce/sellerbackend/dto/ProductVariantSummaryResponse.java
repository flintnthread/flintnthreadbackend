package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductVariantSummaryResponse {

    private Long id;
    private String sku;
    private String color;
    private String size;
    private Integer stock;
    private Integer minQuantity;
    private BigDecimal sellingPrice;
    private BigDecimal finalPrice;
    private BigDecimal metroMetroDeliveryCharge;
    private String image;
}
