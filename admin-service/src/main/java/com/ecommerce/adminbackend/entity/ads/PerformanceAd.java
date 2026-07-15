package com.ecommerce.adminbackend.entity.ads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_ads")
@Getter
@Setter
public class PerformanceAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "pricing_model", nullable = false, length = 50)
    private String pricingModel;

    @Column(name = "low_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "high_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal highPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String status = "active";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "active";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
