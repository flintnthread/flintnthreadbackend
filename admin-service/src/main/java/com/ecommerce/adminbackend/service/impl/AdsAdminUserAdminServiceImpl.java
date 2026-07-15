package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.ads.AdsAdminUser;
import com.ecommerce.adminbackend.repository.AdsAdminUserRepository;
import com.ecommerce.adminbackend.service.AdsAdminUserAdminService;
import com.ecommerce.adminbackend.service.support.AdminPasswordSupport;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdsAdminUserAdminServiceImpl extends BaseAdminService implements AdsAdminUserAdminService {

    private static final Set<String> ROLES = Set.of("admin", "manager", "viewer");
    private static final Set<String> STATUSES = Set.of("active", "inactive");

    private final AdsAdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String search, String status) {
        return repository.search(blankToNull(search), blankToNull(status)).stream().map(this::toMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Ads admin user not found."));
    }

    private String normalizeEmail(String email) {
        String normalized = requireNonBlank(email, "email").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        return normalized;
    }

    private String normalizePassword(String password, boolean required) {
        if (password == null || password.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("password is required.");
            }
            return null;
        }
        String trimmed = password.trim();
        if (trimmed.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        return trimmed;
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String username = requireNonBlank(stringAt(body, "username"), "username").trim();
        String email = normalizeEmail(stringAt(body, "email"));
        String password = normalizePassword(stringAt(body, "password"), true);
        String fullName = body.containsKey("fullName")
                ? requireNonBlank(stringAt(body, "fullName"), "fullName")
                : body.containsKey("full_name")
                ? requireNonBlank(stringAt(body, "full_name"), "fullName")
                : requireNonBlank(stringAt(body, "name"), "fullName");

        if (username.length() > 50) {
            throw new IllegalArgumentException("Username must be 50 characters or fewer.");
        }
        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException("Full name must be 100 characters or fewer.");
        }
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        AdsAdminUser user = new AdsAdminUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName.trim());
        user.setPassword(AdminPasswordSupport.encodeIfNeeded(passwordEncoder, password));
        user.setAvatar(stringAt(body, "avatar"));
        user.setRole(normalizeRole(stringAt(body, "role")));
        user.setStatus(normalizeStatus(stringAt(body, "status")));
        return toMap(repository.save(user));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, Map<String, Object> body) {
        AdsAdminUser user = requireFound(repository.findById(id), "Ads admin user not found.");
        if (body.containsKey("username")) {
            String username = requireNonBlank(stringAt(body, "username"), "username").trim();
            if (username.length() > 50) {
                throw new IllegalArgumentException("Username must be 50 characters or fewer.");
            }
            repository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Username is already taken.");
                }
            });
            user.setUsername(username);
        }
        if (body.containsKey("email")) {
            String email = normalizeEmail(stringAt(body, "email"));
            if (repository.existsByEmailIgnoreCase(email)
                    && !email.equalsIgnoreCase(user.getEmail())) {
                throw new IllegalArgumentException("Email is already registered.");
            }
            user.setEmail(email);
        }
        if (body.containsKey("fullName") || body.containsKey("full_name") || body.containsKey("name")) {
            String fullName = body.containsKey("fullName")
                    ? stringAt(body, "fullName")
                    : body.containsKey("full_name")
                    ? stringAt(body, "full_name")
                    : stringAt(body, "name");
            String trimmed = requireNonBlank(fullName, "fullName").trim();
            if (trimmed.length() > 100) {
                throw new IllegalArgumentException("Full name must be 100 characters or fewer.");
            }
            user.setFullName(trimmed);
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(stringAt(body, "avatar"));
        }
        if (body.containsKey("role")) {
            user.setRole(normalizeRole(stringAt(body, "role")));
        }
        if (body.containsKey("status")) {
            user.setStatus(normalizeStatus(stringAt(body, "status")));
        }
        if (body.containsKey("password")) {
            String password = normalizePassword(stringAt(body, "password"), false);
            if (password != null) {
                user.setPassword(AdminPasswordSupport.encodeIfNeeded(passwordEncoder, password));
            }
        }
        return toMap(repository.save(user));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireFound(repository.findById(id), "Ads admin user not found.");
        repository.deleteById(id);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "admin";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw new IllegalArgumentException("role must be admin, manager, or viewer.");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("status must be active or inactive.");
        }
        return normalized;
    }

    private Map<String, Object> toMap(AdsAdminUser user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("fullName", user.getFullName());
        row.put("avatar", user.getAvatar());
        row.put("role", user.getRole());
        row.put("status", user.getStatus());
        row.put("lastLogin", user.getLastLogin());
        row.put("createdAt", user.getCreatedAt());
        row.put("updatedAt", user.getUpdatedAt());
        return row;
    }
}
