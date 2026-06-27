package com.ecommerce.sellerbackend.dto;


import lombok.Data;

@Data
    public class SellerGstDetailsDto {

        private Integer sellerId;

        private String gstin;

        private String legalName;

        private String tradeName;

        private String gstStatus;

        private String taxpayerType;

        private String constitution;

        private String registrationDate;

        private String cancellationDate;

        private String stateJurisdiction;

        private String centreJurisdiction;

        private String principalPlace;

        private String pan;

        private String address;

        private String city;

        private String state;

        private String pincode;

        private Boolean verified;
    }

