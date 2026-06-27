package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IfscLookupResponse {
    private String ifsc;
    private String bank;
    private String branch;
    private String city;
    private String state;
    private boolean found;
}
