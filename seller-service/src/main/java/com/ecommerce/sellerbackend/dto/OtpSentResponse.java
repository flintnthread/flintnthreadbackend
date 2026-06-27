package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OtpSentResponse {

    private String message;
    private String maskedMobile;
    /** Present only when app.otp.dev-mode=true (local testing). */
    private String devOtp;
}
