package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResponse {

    private String message;
    private boolean verified;
    private String email;
    private Long sellerId;
}
