package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {

    private String accessToken;
    private Long expiresIn;
}
