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

        private String firstName;
        private String lastName;

    @Column(name = "mobile", unique = true)   // 👈 IMPORTANT FIX
    private String mobileNumber;


    @Column(unique = true)
        private String email;

        private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Registered business name (often matches a Shiprocket pickup nickname). */
    @Column(name = "business_name", insertable = false, updatable = false)
    private String businessName;

    @Column(name = "branch_name", insertable = false, updatable = false)
    private String branchName;

    @Column(name = "address", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String address;

    @Column(name = "warehouse_address", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String warehouseAddress;

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



