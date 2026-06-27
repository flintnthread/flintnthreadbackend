package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
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
    private Integer sellerId;

    @Column(name = "chart_name")
    private String chartName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "subcategory_id")
    private Integer subcategoryId;

    @Column(name = "chart_data", columnDefinition = "LONGTEXT", nullable = false)
    private String chartData;

    @Column(name = "chart_image")
    private String chartImage;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
