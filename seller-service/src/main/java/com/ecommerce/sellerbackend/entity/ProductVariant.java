package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private String color;

    private String size;

    private String sku;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "mrp_excl_gst")
    private BigDecimal mrpExclGst;

    private Integer stock;

    @Column(name = "mrp_price")
    private BigDecimal mrpPrice;

    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "mrp_incl_gst")
    private BigDecimal mrpInclGst;

    @Column(name = "intra_city_delivery_charge")
    private BigDecimal intraCityDeliveryCharge;

    @Column(name = "metro_metro_delivery_charge")
    private BigDecimal metroMetroDeliveryCharge;

    @Column(name = "total_price_intra_city")
    private BigDecimal totalPriceIntraCity;

    @Column(name = "total_price_metro_metro")
    private BigDecimal totalPriceMetroMetro;

    @Column(name = "commission_percentage")
    private BigDecimal commissionPercentage;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "video_path")
    private String videoPath;

    private BigDecimal weight;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
