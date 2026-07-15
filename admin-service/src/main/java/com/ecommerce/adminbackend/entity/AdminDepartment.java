package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to existing {@code career_departments} (legacy careers schema).
 */
@Entity
@Table(name = "career_departments")
@Getter
@Setter
public class AdminDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** DB enum: active | inactive */
    @Column(length = 20)
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** API convenience — not a DB column. */
    @Transient
    private Boolean active;

    public Boolean getActive() {
        if (active != null) {
            return active;
        }
        return status == null || "active".equalsIgnoreCase(status.trim());
    }

    public void setActive(Boolean active) {
        this.active = active;
        if (active != null) {
            this.status = Boolean.TRUE.equals(active) ? "active" : "inactive";
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = Boolean.FALSE.equals(active) ? "inactive" : "active";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
