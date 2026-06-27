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
public class UpdateProductRequest {

    private Integer categoryId;
    private String categoryName;
    private Integer subcategoryId;
    private String subcategoryName;
    private Integer childCategoryId;
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
    private List<UpdateProductVariantRequest> variants = new ArrayList<>();

    @Valid
    private List<CreateProductImageRequest> images = new ArrayList<>();

    public CreateProductRequest toCreatePayload() {
        CreateProductRequest payload = new CreateProductRequest();
        payload.setCategoryId(categoryId);
        payload.setCategoryName(categoryName);
        payload.setSubcategoryId(subcategoryId);
        payload.setSubcategoryName(subcategoryName);
        payload.setChildCategoryId(childCategoryId);
        payload.setMiddleCategoryName(middleCategoryName);
        payload.setName(name);
        payload.setSku(sku);
        payload.setHsnCode(hsnCode);
        payload.setProductMaterialType(productMaterialType);
        payload.setGstPercentage(gstPercentage);
        payload.setLengthCm(lengthCm);
        payload.setWidthCm(widthCm);
        payload.setHeightCm(heightCm);
        payload.setProductWeight(productWeight);
        payload.setFragile(fragile);
        payload.setShortDescription(shortDescription);
        payload.setDescription(description);
        payload.setFeatures(features);
        payload.setReturnPolicy(returnPolicy);
        payload.setSpecifications(specifications);
        payload.setSizeChartId(sizeChartId);
        payload.setDeliveryTimeMin(deliveryTimeMin);
        payload.setDeliveryTimeMax(deliveryTimeMax);
        payload.setDeliveryInfo(deliveryInfo);
        payload.setWarrantyInfo(warrantyInfo);
        payload.setCareInstructions(careInstructions);
        payload.setAcceptCod(acceptCod);
        payload.setAcceptPrepaid(acceptPrepaid);
        payload.setCustomized(customized);
        payload.setCustomTitle(customTitle);
        payload.setCustomInstructions(customInstructions);
        payload.setCustomLeadDays(customLeadDays);
        payload.setCustomCharge(customCharge);
        payload.setCustomAllowPhoto(customAllowPhoto);
        payload.setCustomImageLabel(customImageLabel);
        payload.setCustomAllowText(customAllowText);
        payload.setCustomTextLabel(customTextLabel);
        payload.setImages(images);
        payload.setVariants(variants.stream().map(v -> (CreateProductVariantRequest) v).toList());
        return payload;
    }
}
