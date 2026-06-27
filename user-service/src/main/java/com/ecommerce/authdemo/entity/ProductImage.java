package com.ecommerce.authdemo.entity;

import com.ecommerce.authdemo.dto.ProductImageDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
    @Entity
    @Table(name = "product_images")
    public class ProductImage {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name="variant_id")
        private Long variantId;

        @Column(name="image_path")
        private String imagePath;

        @Column(name="is_primary")
        private Boolean isPrimary;

        @Column(name="sort_order")
        private Integer sortOrder;

        @Column(name="created_at")
        private LocalDateTime createdAt;

        @ManyToOne
        @JoinColumn(name = "product_id")
        @JsonIgnore
        private Product product;

    }

