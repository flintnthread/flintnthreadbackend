package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_users")
@Getter
@Setter
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role = AdminRole.admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminAccountStatus status = AdminAccountStatus.active;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** API / service alias for name */
    public String getFullName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    /** API / service alias for password */
    public String getPasswordHash() {
        return password;
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public Boolean getActive() {
        return status == AdminAccountStatus.active;
    }

    public void setActive(Boolean active) {
        this.status = Boolean.TRUE.equals(active) ? AdminAccountStatus.active : AdminAccountStatus.inactive;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLogin;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLogin = lastLoginAt;
    }
}
