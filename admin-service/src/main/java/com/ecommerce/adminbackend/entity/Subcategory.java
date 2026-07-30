package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "subcategory_name", nullable = false)
    private String subcategoryName;

    @Column(name = "subcategory_image")
    private String subcategoryImage;

    @Column(name = "material_slabs", columnDefinition = "text")
    private String materialSlabs;

    @Column(name = "weight_slabs", columnDefinition = "text")
    private String weightSlabs;

    @Column(name = "gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name = "status", nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "mobile_image")
    private String mobileImage;
}
