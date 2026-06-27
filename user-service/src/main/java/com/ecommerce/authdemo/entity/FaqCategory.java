package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "category_icon", length = 255)
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
