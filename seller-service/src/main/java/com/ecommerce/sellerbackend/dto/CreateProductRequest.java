package com.ecommerce.sellerbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateProductRequest {

    private Integer categoryId;

    private String categoryName;

    private Integer subcategoryId;

    private String subcategoryName;

    /** Normal category row in {@code categories} where {@code parent_id} points to main. */
    private Integer childCategoryId;

    /** Normal category name (e.g. Bags) — used when {@code subcategoryId} is not yet resolved. */
    private String middleCategoryName;

    @NotBlank
    private String name;

    private String sku;

    @NotBlank
    private String hsnCode;

    private String productMaterialType;

    private BigDecimal gstPercentage;

    @NotNull
    private BigDecimal lengthCm;

    @NotNull
    private BigDecimal widthCm;

    @NotNull
    private BigDecimal heightCm;

    @NotNull
    private BigDecimal productWeight;

    private Boolean fragile;

    @NotBlank
    private String shortDescription;

    @NotBlank
    private String description;

    private String features;

    @NotBlank
    private String returnPolicy;

    private String specifications;

    private Integer sizeChartId;

    private Integer deliveryTimeMin;

    private Integer deliveryTimeMax;

    private String deliveryInfo;

    private String warrantyInfo;

    private String careInstructions;

    private Boolean acceptCod;

    private Boolean acceptPrepaid;

    private Boolean customized;

    private String customTitle;

    private String customInstructions;

    private String customLeadDays;

    private String customCharge;

    private Boolean customAllowPhoto;

    private String customImageLabel;

    private Boolean customAllowText;

    private String customTextLabel;

    @Valid
    @NotEmpty
    private List<CreateProductVariantRequest> variants = new ArrayList<>();

    @Valid
    private List<CreateProductImageRequest> images = new ArrayList<>();
}
