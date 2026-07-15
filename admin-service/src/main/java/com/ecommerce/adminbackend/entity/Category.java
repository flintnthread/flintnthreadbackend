package com.ecommerce.adminbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "category_image")
    private String categoryImage;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name = "status", nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description")
    private String seoDescription;

    @Column(name = "seo_keywords")
    private String seoKeywords;

    @Column(name = "seo_content", columnDefinition = "mediumtext")
    private String seoContent;

    @Column(name = "url_slug")
    private String urlSlug;

    @Column(name = "seo_manual_override")
    private Boolean seoManualOverride = false;

    /** Not present on all DB deployments; keep out of generated SQL. */
    @jakarta.persistence.Transient
    private String mobileImage;

    /** Not present on all DB deployments; keep out of generated SQL. */
    @jakarta.persistence.Transient
    private String bannerImage;
}
