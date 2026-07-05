package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductListItemResponse {

    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal mrpInclGst;
    private String image;
    private String status;
    private Integer stock;
    private String updated;
    private Integer categoryId;
    private String category;
    /** Middle level category name (between main category and leaf subcategory). */
    private String categorySub;
    private Integer subcategoryId;
    private String subcategory;
    private String color;
    private String size;
    private Integer minQuantity;
    private String description;
    private String material;
    private String weight;
    private String dimensions;
    private String returnPolicy;
    private String warranty;
    private List<ProductVariantSummaryResponse> variants;
}
