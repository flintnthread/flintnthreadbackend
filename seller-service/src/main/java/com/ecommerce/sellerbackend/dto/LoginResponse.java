package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private Long sellerId;
    private String email;
    private String mobile;
    private String firstName;
    private String lastName;
    private String businessName;
    private boolean emailVerified;
    private boolean profileCompleted;
    private String status;
    private boolean subscriptionActive;
    private boolean paymentPending;
    private String subscriptionExpiresAt;
    private String accessToken;
    private Long expiresIn;
}
