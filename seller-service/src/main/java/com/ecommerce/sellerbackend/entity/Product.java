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
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "subcategory_id", nullable = false)
    private Integer subcategoryId;

    @Column(name = "size_chart_id")
    private Integer sizeChartId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "product_material_type")
    private String productMaterialType;

    @Column(name = "gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name = "length_cm")
    private BigDecimal lengthCm;

    @Column(name = "width_cm")
    private BigDecimal widthCm;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "is_fragile")
    private Boolean fragile;

    @Column(nullable = false)
    private String name;

    private String sku;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "return_policy", columnDefinition = "TEXT")
    private String returnPolicy;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    private String status;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "delivery_time_min")
    private Integer deliveryTimeMin;

    @Column(name = "delivery_time_max")
    private Integer deliveryTimeMax;

    @Column(name = "delivery_info", columnDefinition = "TEXT")
    private String deliveryInfo;

    @Column(name = "warranty_info", columnDefinition = "TEXT")
    private String warrantyInfo;

    @Column(name = "care_instructions", columnDefinition = "TEXT")
    private String careInstructions;

    @Column(name = "product_weight")
    private BigDecimal productWeight;

    @Column(name = "intra_city_charge")
    private BigDecimal intraCityCharge;

    @Column(name = "metro_metro_charge")
    private BigDecimal metroMetroCharge;

    @Column(name = "accept_cod", nullable = false)
    private Boolean acceptCod;

    @Column(name = "deliver_all_locations", nullable = false)
    private Boolean deliverAllLocations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
