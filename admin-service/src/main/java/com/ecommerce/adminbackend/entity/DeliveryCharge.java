package com.ecommerce.adminbackend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_charges")
@Getter
@Setter
public class DeliveryCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "weight_slab", nullable = false, length = 50)
    @JsonAlias("label")
    private String weightSlab;

    @Column(name = "weight_min", nullable = false, precision = 8, scale = 3)
    @JsonAlias("minWeightKg")
    private BigDecimal weightMin;

    @Column(name = "weight_max", nullable = false, precision = 8, scale = 3)
    @JsonAlias("maxWeightKg")
    private BigDecimal weightMax;

    @Column(name = "intra_city_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal intraCityCharge;

    @Column(name = "metro_metro_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal metroMetroCharge;

    @Column(name = "is_custom", nullable = false)
    @JsonAlias("custom")
    private Boolean custom = false;

    @Column(nullable = false)
    @JsonProperty("active")
    private Boolean status = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
