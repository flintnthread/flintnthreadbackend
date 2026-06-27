package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "subcategories")
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "subcategory_name")
    private String subcategoryName;

    @Column(name = "subcategory_image")
    private String subcategoryImage;

    @Column(name = "material_slabs")
    private String materialSlabs;

    @Column(name = "weight_slabs")
    private String weightSlabs;

    @Column(name = "gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "mobile_image")   // ✅ MATCH DB
    private String mobileImage;
}