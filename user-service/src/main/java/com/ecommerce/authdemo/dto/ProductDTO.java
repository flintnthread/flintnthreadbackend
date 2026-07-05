package com.ecommerce.authdemo.dto;

import jakarta.persistence.Entity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDTO {

    private Long id;
    private Long categoryId;
    private Long subcategoryId;
    private Long sellerId;

    /** Resolved seller store label for product detail (business name when available). */
    private String sellerBusinessName;

    private Boolean buyNowEnabled;

    /** Maps to products.is_customized_product (1 = post-order customization). */
    private Boolean isCustomizedProduct;

    /** Alias for mobile clients (same value as isCustomizedProduct). */
    private Boolean isCustomized;

    /** JSON array defining required customization fields after order placement. */
    private String customRequiredFields;

    private String name;
    private String sku;
    private String hsnCode;
    private String productMaterialType;

    private String shortDescription;
    private String description;
    private String features;
    private String specifications;
    private String returnPolicy;

    /** FK to existing {@code size_charts} table. */
    private Integer sizeChartId;
    /** Normalized JSON for product detail UI (headers + rows). */
    private String sizeChart;
    private String sizeChartUnit;
    private String sizeChartName;
    private String sizeChartImage;

    private BigDecimal gstPercentage;
    private BigDecimal productWeight;

    private Integer deliveryTimeMin;
    private Integer deliveryTimeMax;

    private String status;
    private LocalDateTime createdAt;

    // 🔥 RELATIONS
    private List<ProductImageDTO> images;
    private List<ProductVariantDTO> variants;

    // Dynamic ratings from active reviews
    private Double rating;
    private Long ratingCount;

}