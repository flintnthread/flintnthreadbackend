package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "refund_transactions")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class RefundTransaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "order_id")
        private Long orderId;

        @Column(name = "return_id")
        private Long returnId;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "payment_method")
        private String paymentMethod;

        @Column(name = "refund_type")
        private String refundType;

        @Column(name = "refund_amount")
        private Double refundAmount;

        @Column(name = "refund_status")
        private String refundStatus;

        @Column(name = "razorpay_refund_id")
        private String razorpayRefundId;

        @Column(name = "refund_reference")
        private String refundReference;

        @Column(name = "remarks")
        private String remarks;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        public void prePersist() {

            this.createdAt = LocalDateTime.now();

            this.updatedAt = LocalDateTime.now();

            if (this.refundStatus == null) {
                this.refundStatus = "pending";
            }
        }

        @PreUpdate
        public void preUpdate() {

            this.updatedAt = LocalDateTime.now();
        }
    }

