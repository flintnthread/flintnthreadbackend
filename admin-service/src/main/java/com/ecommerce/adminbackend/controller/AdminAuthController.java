package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.dto.auth.AdminLoginRequest;
import com.ecommerce.adminbackend.dto.auth.AdminLoginResponse;
import com.ecommerce.adminbackend.dto.auth.AdminMeResponse;
import com.ecommerce.adminbackend.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private static final Logger log = LogFactory.getLogger(AdminAuthController.class);

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }

    @GetMapping("/me")
    public AdminMeResponse me() {
        return adminAuthService.getCurrentAdmin();
    }
}
