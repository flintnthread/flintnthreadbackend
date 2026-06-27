package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.dto.auth.AdminLoginRequest;
import com.ecommerce.adminbackend.dto.auth.AdminLoginResponse;
import com.ecommerce.adminbackend.dto.auth.AdminMeResponse;
import com.ecommerce.adminbackend.entity.AdminUser;
import com.ecommerce.adminbackend.exception.UnauthorizedException;
import com.ecommerce.adminbackend.repository.AdminUserRepository;
import com.ecommerce.adminbackend.security.AdminJwtService;
import com.ecommerce.adminbackend.security.AdminSecurityUtils;
import com.ecommerce.adminbackend.entity.AdminAccountStatus;
import com.ecommerce.adminbackend.service.AdminAuthService;
import com.ecommerce.adminbackend.service.support.AdminPasswordSupport;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl extends BaseAdminService implements AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService adminJwtService;

    @Override
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        String email = requireNonBlank(request.getEmail(), "Email").toLowerCase();
        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (admin.getStatus() != AdminAccountStatus.active) {
            throw new UnauthorizedException("This admin account is disabled.");
        }
        if (!AdminPasswordSupport.matches(passwordEncoder, request.getPassword(), admin.getPassword())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(admin);

        log.info("Admin login successful: adminId={}, email={}", admin.getId(), admin.getEmail());

        return AdminLoginResponse.builder()
                .adminId(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole().name())
                .accessToken(adminJwtService.generateAccessToken(
                        admin.getId(), admin.getEmail(), admin.getRole().name()))
                .expiresInSeconds(adminJwtService.getExpirySeconds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMeResponse getCurrentAdmin() {
        AdminUser admin = requireFound(
                adminUserRepository.findById(AdminSecurityUtils.currentAdminId()),
                "Admin user not found.");
        return AdminMeResponse.builder()
                .adminId(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole().name())
                .active(Boolean.TRUE.equals(admin.getActive()))
                .build();
    }
}
