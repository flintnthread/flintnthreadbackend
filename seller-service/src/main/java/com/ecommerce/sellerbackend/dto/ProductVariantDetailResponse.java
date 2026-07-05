package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductVariantDetailResponse {

    private Long id;
    private Long productId;
    private String color;
    private String colorHex;
    private Long colorId;
    private String size;
    private Long sizeId;
    private String sku;
    private Integer stock;
    private Integer minQuantity;

    private BigDecimal basePrice;
    private BigDecimal mrpExclGst;
    private BigDecimal mrpPrice;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal sellingPrice;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal finalPrice;
    private BigDecimal mrpInclGst;
    private BigDecimal intraCityDeliveryCharge;
    private BigDecimal metroMetroDeliveryCharge;
    private BigDecimal totalPriceIntraCity;
    private BigDecimal totalPriceMetroMetro;
    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;
    private String videoPath;
    private BigDecimal weight;
    private String createdAt;
    private String updatedAt;

    /** UI-friendly aliases */
    private BigDecimal mrp;
    private Integer discount;
    private BigDecimal sellingPriceExGst;
    private BigDecimal gstPercent;
    private BigDecimal gstAmount;
    private BigDecimal sellingPriceWithGst;
    private BigDecimal commissionPercent;
    private BigDecimal intraCityDelivery;
    private BigDecimal metroMetroDelivery;
    private BigDecimal totalIntraCity;
    private BigDecimal totalMetroMetro;
    private String imageUri;
    private String videoUri;
}
