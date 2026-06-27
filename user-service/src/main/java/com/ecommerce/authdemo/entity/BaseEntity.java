package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

    @MappedSuperclass
    @Getter
    @Setter
    public abstract class BaseEntity {

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @Column(name = "created_by")
        private Long createdBy;

        @Column(name = "updated_by")
        private Long updatedBy;

        @PrePersist
        protected void onCreate() {
            createdAt = LocalDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
            updatedAt = LocalDateTime.now();
        }
    }

