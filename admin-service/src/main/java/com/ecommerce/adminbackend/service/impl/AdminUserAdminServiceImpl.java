package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.AdminAccountStatus;
import com.ecommerce.adminbackend.entity.AdminRole;
import com.ecommerce.adminbackend.entity.AdminUser;
import com.ecommerce.adminbackend.service.support.AdminPasswordSupport;
import com.ecommerce.adminbackend.repository.AdminUserRepository;
import com.ecommerce.adminbackend.service.AdminUserAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserAdminServiceImpl extends BaseAdminService implements AdminUserAdminService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listUsers(int page, int size) {
        var result = adminUserRepository.findAll(PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toUser));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUser(Long id) {
        return toUser(requireUser(id));
    }

    @Override
    @Transactional
    public Map<String, Object> createUser(AdminUser input, String rawPassword) {
        String email = requireNonBlank(input.getEmail(), "Email").toLowerCase();
        requireNonBlank(rawPassword, "Password");
        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        AdminUser user = new AdminUser();
        user.setEmail(email);
        user.setName(input.getFullName() != null ? input.getFullName().trim() : email);
        user.setUsername(email.split("@")[0]);
        user.setRole(input.getRole() != null ? input.getRole() : AdminRole.admin);
        user.setStatus(Boolean.FALSE.equals(input.getActive()) ? AdminAccountStatus.inactive : AdminAccountStatus.active);
        user.setPassword(AdminPasswordSupport.encodeIfNeeded(passwordEncoder, rawPassword));
        return toUser(adminUserRepository.save(user));
    }

    @Override
    @Transactional
    public Map<String, Object> updateUser(Long id, AdminUser input, String rawPassword) {
        AdminUser user = requireUser(id);
        if (input.getFullName() != null) {
            user.setName(input.getFullName().trim());
        }
        if (input.getRole() != null) {
            user.setRole(input.getRole());
        }
        if (input.getActive() != null) {
            user.setActive(input.getActive());
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(AdminPasswordSupport.encodeIfNeeded(passwordEncoder, rawPassword));
        }
        return toUser(adminUserRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        requireUser(id);
        adminUserRepository.deleteById(id);
    }

    private AdminUser requireUser(Long id) {
        return requireFound(adminUserRepository.findById(id), "Admin user not found.");
    }

    private Map<String, Object> toUser(AdminUser user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("email", user.getEmail());
        row.put("fullName", user.getFullName());
        row.put("role", user.getRole().name());
        row.put("active", user.getActive());
        row.put("lastLoginAt", user.getLastLoginAt());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }
}
