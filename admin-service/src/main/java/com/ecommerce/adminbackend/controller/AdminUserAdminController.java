package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.adminuser.AdminUserRequest;
import com.ecommerce.adminbackend.entity.AdminUser;
import com.ecommerce.adminbackend.service.AdminUserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserAdminController {

    private static final Logger log = LogFactory.getLogger(AdminUserAdminController.class);

    private final AdminUserAdminService adminUserAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserAdminService.listUsers(page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return adminUserAdminService.getUser(id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody AdminUserRequest request) {
        AdminUser user = new AdminUser();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setActive(request.getActive());
        return adminUserAdminService.createUser(user, request.getPassword());
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        AdminUser user = new AdminUser();
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setActive(request.getActive());
        return adminUserAdminService.updateUser(id, user, request.getPassword());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminUserAdminService.deleteUser(id);
    }
}
