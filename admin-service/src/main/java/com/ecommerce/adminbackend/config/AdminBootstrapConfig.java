package com.ecommerce.adminbackend.config;

import com.ecommerce.adminbackend.entity.AdminAccountStatus;
import com.ecommerce.adminbackend.entity.AdminRole;
import com.ecommerce.adminbackend.entity.AdminUser;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.repository.AdminUserRepository;
import com.ecommerce.adminbackend.service.support.AdminPasswordSupport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapConfig implements ApplicationRunner {

    private static final Logger log = LogFactory.getLogger(AdminBootstrapConfig.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.email:admin@flintnthread.in}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${app.admin.bootstrap.full-name:Platform Admin}")
    private String bootstrapFullName;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.debug("Admin bootstrap disabled.");
            return;
        }
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.warn("Admin bootstrap skipped: app.admin.bootstrap.password is empty.");
            return;
        }

        String email = bootstrapEmail.trim().toLowerCase();
        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            log.debug("Bootstrap admin already exists for {}", email);
            return;
        }

        String username = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setUsername(username);
        admin.setName(bootstrapFullName);
        admin.setPassword(AdminPasswordSupport.encodeIfNeeded(passwordEncoder, bootstrapPassword));
        admin.setRole(AdminRole.super_admin);
        admin.setStatus(AdminAccountStatus.active);
        adminUserRepository.save(admin);

        log.info("Created bootstrap admin account for {}", admin.getEmail());
    }
}
