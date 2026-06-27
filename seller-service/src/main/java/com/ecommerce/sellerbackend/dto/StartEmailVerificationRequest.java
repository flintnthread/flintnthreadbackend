package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartEmailVerificationRequest {

    @NotBlank(message = "Verification token is required")
    private String token;
}
