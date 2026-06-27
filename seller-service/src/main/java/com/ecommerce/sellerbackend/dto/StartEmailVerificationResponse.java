package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StartEmailVerificationResponse {

    private final String message;
    private final String email;
    private final boolean otpSent;
    private final boolean alreadyVerified;
}
