package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Lob
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** Cloudinary (and similar) URLs are often longer than 255 chars. */
    @Column(name = "image_path", length = 2048)
    private String imagePath;

    @Column(name = "status", nullable = false)
    private Boolean status = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

