package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.AdminUser;

import java.util.Map;

public interface AdminUserAdminService {

    PageResponse<Map<String, Object>> listUsers(int page, int size);

    Map<String, Object> getUser(Long id);

    Map<String, Object> createUser(AdminUser input, String rawPassword);

    Map<String, Object> updateUser(Long id, AdminUser input, String rawPassword);

    void deleteUser(Long id);
}
