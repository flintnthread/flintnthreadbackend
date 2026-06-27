package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    // Find admin by email (used for login)
    Optional<AdminUser> findByEmail(String email);

    // Check if admin exists
    boolean existsByEmail(String email);
}