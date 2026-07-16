package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pincodes")
@Getter
@Setter
public class DeliveryPincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String pincode;

    /** Matches `pincodes.status` (boolean) used by location APIs. */
    private Boolean status;

    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "state_id")
    private Integer stateId;

    @Column(name = "country_id")
    private Integer countryId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}