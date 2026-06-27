package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantDTO {

    private Long id;

    private Long productId;

    private String color;
    private String size;
    private String sku;

    private BigDecimal mrpPrice;
    private BigDecimal sellingPrice;
    private BigDecimal finalPrice;

    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;

    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;

    private Integer stock;
    private Boolean inStock;

    private String videoPath;
    private BigDecimal weight;
}