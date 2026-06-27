package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "payment_transactions")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class PaymentTransaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        // 🔗 Order reference (assuming order_id exists in orders table)
        @Column(name = "order_id", nullable = false)
        private Integer orderId;

        // Razorpay payment id / transaction id
        @Column(name = "transaction_id", length = 255)
        private String transactionId;

        @Column(name = "payment_method", nullable = false, length = 50)
        private String paymentMethod;

        @Column(name = "amount", nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Column(name = "currency", nullable = false, length = 10)
        @Builder.Default
        private String currency = "INR";

        @Column(name = "status", nullable = false, length = 50)
        private String status;

        @Column(name = "response_data", columnDefinition = "TEXT")
        private String responseData;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", insertable = false, updatable = false)
        private LocalDateTime updatedAt;
    }

