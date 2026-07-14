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

    /** Strike / list MRP for UI (admin MRP, scaled to customer unit when applicable). */
    private BigDecimal mrpPrice;
    /** Admin panel MRP excl. GST ({@code mrp_excl_gst}). */
    private BigDecimal mrpExclGst;
    /** Admin panel MRP incl. GST ({@code mrp_incl_gst}). */
    private BigDecimal mrpInclGst;
    private BigDecimal sellingPrice;
    private BigDecimal sellingPriceExclGst;
    private BigDecimal finalPrice;
    private BigDecimal customerPrice;

    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;

    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;

    private BigDecimal intraCityDeliveryCharge;
    private BigDecimal metroMetroDeliveryCharge;
    private BigDecimal deliveryCharge;
    private BigDecimal totalPriceIntraCity;
    private BigDecimal totalPriceMetroMetro;

    private Integer stock;
    private Boolean inStock;

    private String videoPath;
    private BigDecimal weight;
}