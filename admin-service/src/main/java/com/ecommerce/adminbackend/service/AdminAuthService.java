package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.dto.auth.AdminLoginRequest;
import com.ecommerce.adminbackend.dto.auth.AdminLoginResponse;
import com.ecommerce.adminbackend.dto.auth.AdminMeResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);

    AdminMeResponse getCurrentAdmin();
}
