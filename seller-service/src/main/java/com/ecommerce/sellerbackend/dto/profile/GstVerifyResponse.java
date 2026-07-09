package com.ecommerce.sellerbackend.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GstVerifyResponse {
    @JsonProperty("verified")
    private final boolean verified;
    /** True when this GSTIN is already registered to another seller account. */
    @JsonProperty("alreadyExists")
    private final boolean alreadyExists;
    private final String gstNumber;
    private final String message;
    private final String businessName;
    private final String tradeName;
    private final String businessType;
    private final String panNumber;
    private final String address;
    private final String city;
    private final String state;
    private final String pincode;
    /** Active, Cancelled, Suspended, etc. */
    private final String status;
    /** Regular, Composition, etc. */
    private final String taxpayerType;
    private final String registrationDate;
    private final String cancellationDate;
    private final String stateJurisdiction;
    private final String centreJurisdiction;
    private final String principalPlaceType;
}
