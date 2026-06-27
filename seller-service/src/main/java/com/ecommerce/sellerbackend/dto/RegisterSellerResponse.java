package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterSellerResponse {

    private Long sellerId;
    private String message;
    private boolean emailVerificationRequired;
}
