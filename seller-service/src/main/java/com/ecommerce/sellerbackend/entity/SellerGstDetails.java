package com.ecommerce.sellerbackend.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
    @Table(name = "seller_gst_details")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class SellerGstDetails {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "seller_id")
        private Integer sellerId;

        @Column(name = "gstin")
        private String gstin;

        @Column(name = "legal_name")
        private String legalName;

        @Column(name = "trade_name")
        private String tradeName;

        @Column(name = "gst_status")
        private String gstStatus;

        @Column(name = "taxpayer_type")
        private String taxpayerType;

        @Column(name = "constitution")
        private String constitution;

        @Column(name = "registration_date")
        private String registrationDate;

        @Column(name = "cancellation_date")
        private String cancellationDate;

        @Column(name = "state_jurisdiction")
        private String stateJurisdiction;

        @Column(name = "centre_jurisdiction")
        private String centreJurisdiction;

        @Column(name = "principal_place")
        private String principalPlace;

        private String pan;

        @Column(columnDefinition = "TEXT")
        private String address;

        private String city;

        private String state;

        private String pincode;

        private Boolean verified;
    }

