package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstVerifyResponse {

    private boolean valid;
    private boolean verified;
    private String message;

    // GST Details
    private String gstNumber;

    // Business Details
    private String businessName;
    private String tradeName;

    // GST Status
    private String status;
    private String taxpayerType;
    private String businessType;

    // Registration Details
    private String registrationDate;
    private String cancellationDate;

    // Jurisdiction
    private String stateJurisdiction;
    private String centreJurisdiction;

    // Principal Place
    private String principalPlaceType;

    // PAN
    private String panNumber;

    // Address
    private String address;
    private String city;
    private String state;
    private String pincode;
}