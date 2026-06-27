package com.ecommerce.adminbackend.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLoginResponse {
    private final Long adminId;
    private final String email;
    private final String fullName;
    private final String role;
    private final String accessToken;
    private final long expiresInSeconds;
}
