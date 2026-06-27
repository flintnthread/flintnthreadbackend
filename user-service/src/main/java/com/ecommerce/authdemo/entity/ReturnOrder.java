package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "return_orders")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ReturnOrder {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "order_id")
        private Long orderId;

        @Column(name = "order_item_id")
        private Long orderItemId;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "reason")
        private String reason;

        @Column(name = "description")
        private String description;

        @Column(name = "solution")
        private String solution;

        @Column(name = "status")
        private String status;

        @Column(name = "admin_comment")
        private String adminComment;

        @Column(name = "processed_by")
        private Long processedBy;

        @Column(name = "processed_at")
        private LocalDateTime processedAt;

        @Column(name = "shiprocket_return_id")
        private String shiprocketReturnId;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        public void prePersist() {

            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();

            if (this.status == null) {
                this.status = "pending";
            }
        }

        @PreUpdate
        public void preUpdate() {
            this.updatedAt = LocalDateTime.now();
        }
    }

