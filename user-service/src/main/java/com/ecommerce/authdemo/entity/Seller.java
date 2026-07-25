package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sellers")
@Data
public class Seller {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

    @Column(name = "mobile", unique = true)   // 👈 IMPORTANT FIX
    private String mobileNumber;


    @Column(unique = true)
        private String email;

        private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Registered business / store name shown on product detail and seller storefront. */
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "warehouse_address", columnDefinition = "TEXT")
    private String warehouseAddress;

    @Column(name = "warehouse_city", length = 100)
    private String warehouseCity;

    @Column(name = "warehouse_state", length = 100)
    private String warehouseState;

    @Column(name = "warehouse_country", length = 100)
    private String warehouseCountry;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "gst_number")
    private String gstNumber;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}



