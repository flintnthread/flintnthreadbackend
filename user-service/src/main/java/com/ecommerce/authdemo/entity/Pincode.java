package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pincodes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "state_id", nullable = false)
    private Integer stateId;

    @Column(name = "city_id", nullable = false)
    private Integer cityId;

    @Column(name = "area_id", nullable = false)
    private Integer areaId;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
