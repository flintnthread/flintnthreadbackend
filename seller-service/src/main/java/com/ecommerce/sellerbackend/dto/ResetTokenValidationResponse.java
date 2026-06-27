package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResetTokenValidationResponse {

    private boolean valid;
    private String emailHint;
    private String message;
}
