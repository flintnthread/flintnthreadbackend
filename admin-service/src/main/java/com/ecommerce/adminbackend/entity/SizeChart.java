package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "size_charts")
@Getter
@Setter
public class SizeChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "chart_name", nullable = false, length = 255)
    private String chartName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "subcategory_id")
    private Integer subcategoryId;

    @Column(name = "chart_data", columnDefinition = "LONGTEXT", nullable = false)
    private String chartData;

    @Column(name = "chart_image", length = 500)
    private String chartImage;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
