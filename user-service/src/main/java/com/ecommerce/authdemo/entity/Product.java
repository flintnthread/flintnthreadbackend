package com.ecommerce.authdemo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String sku;
   
    private String shortDescription;
    private String description;

    private String features;
    private String specifications;
    private String returnPolicy;

    @Column(name = "size_chart_id")
    private Integer sizeChartId;

    private BigDecimal gstPercentage;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    private BigDecimal productWeight;

    private String hsnCode;

    private Integer weightSlabId;

    private Boolean isFragile = false;

    private Integer categoryId;
    private Integer subcategoryId;

    private String gender;



    private BigDecimal rating;
    private Integer ratingCount;


    private Long sellerId;

    private Boolean acceptCod = true;
    private Boolean acceptPrepaid = true;

    private Boolean deliverAllLocations = true;

    /** When 1/true, buyer provides customization details after placing the order. */
    @Column(name = "is_customized_product")
    private Boolean isCustomizedProduct = false;

    /** JSON array of required customization fields (label, type, required). */
    @Column(name = "custom_required_fields", columnDefinition = "TEXT")
    private String customRequiredFields;

    private String status;

    private Integer deliveryTimeMin;
    private Integer deliveryTimeMax;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ------------------------
    // RELATIONS
    // ------------------------

    @BatchSize(size = 32)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductVariant> variants;

    @BatchSize(size = 32)
    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<ProductImage> images;

    @BatchSize(size = 32)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductView> views;
//
//    @BatchSize(size = 32)
//    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
//    private Set<ProductColor> productColors;

//    @BatchSize(size = 32)
//    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
//    private Set<ProductSize> productSizes;
}