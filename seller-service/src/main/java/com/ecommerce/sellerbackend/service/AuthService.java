package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.LoginRequest;
import com.ecommerce.sellerbackend.dto.LoginResponse;
import com.ecommerce.sellerbackend.dto.RefreshTokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String clientIp, String userAgent);

    RefreshTokenResponse refreshSession(String bearerToken);
}
