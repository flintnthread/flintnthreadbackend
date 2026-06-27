package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "category_icon")
    private String categoryIcon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
