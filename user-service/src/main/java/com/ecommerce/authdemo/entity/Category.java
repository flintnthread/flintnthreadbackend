package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="parent_id")
    private Long parentId;

    @Column(name="category_name")
    private String categoryName;


    @Column(name="category_image")
    private String image;

    @Column(name = "banner_image")
    private String bannerImage;

    @Column(name = "mobile_image")
    private String mobileImage;


    @Column(name="hsn_code")
    private String hsnCode;

    @Column(name="gst_percentage")
    private Double gstPercentage;

    private Integer status;

    @Column(name="seller_id")
    private Long sellerId;

    @Column(name="created_at")
    private LocalDateTime createdAt;

}