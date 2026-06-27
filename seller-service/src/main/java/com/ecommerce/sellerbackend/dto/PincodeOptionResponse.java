package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PincodeOptionResponse {
    private Integer pincodeId;
    private String pincode;
    private String area;
    private String city;
    private String state;
    private String country;
    private boolean selected;
}
