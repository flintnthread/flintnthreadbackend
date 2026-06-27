package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OtpVerifiedResponse {

    private boolean verified;
    private String mobileVerificationToken;
    private String message;
}
