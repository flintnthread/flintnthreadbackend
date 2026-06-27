package com.ecommerce.adminbackend.entity;

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
@Table(name = "delivery_weight_slabs")
@Getter
@Setter
public class DeliveryWeightSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "min_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal maxWeightKg;

    @Column(name = "intra_city_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal intraCityCharge;

    @Column(name = "metro_metro_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal metroMetroCharge;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
