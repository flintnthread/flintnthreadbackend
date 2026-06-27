package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pincodes")
@Getter
@Setter
public class Pincode {

    @Id
    private Integer id;

    @Column(name = "area_id", nullable = false)
    private Integer areaId;

    @Column(nullable = false, length = 10)
    private String pincode;

    private Boolean status;
}
