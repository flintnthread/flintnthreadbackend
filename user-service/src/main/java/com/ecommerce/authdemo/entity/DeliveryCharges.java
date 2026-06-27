package com.ecommerce.authdemo.entity;





import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "delivery_charges")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class DeliveryCharges {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "weight_slab", nullable = false, length = 50)
        private String weightSlab;

        @Column(name = "weight_min", nullable = false, precision = 8, scale = 3)
        private BigDecimal weightMin;

        @Column(name = "weight_max", nullable = false, precision = 8, scale = 3)
        private BigDecimal weightMax;

        @Column(name = "intra_city_charge", nullable = false, precision = 10, scale = 2)
        private BigDecimal intraCityCharge;

        @Column(name = "metro_metro_charge", nullable = false, precision = 10, scale = 2)
        private BigDecimal metroMetroCharge;

        @Column(name = "is_custom", nullable = false)
        private Boolean isCustom;

        @Column(name = "status", nullable = false)
        private Boolean status;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", insertable = false, updatable = false)
        private LocalDateTime updatedAt;
    }

