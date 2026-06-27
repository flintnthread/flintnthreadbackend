package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name="referral_transactions")
    @Getter
    @Setter
    public class ReferralTransaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private Long referrerId;

        private Long referredUserId;

        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        private TransactionType transactionType;

        @Enumerated(EnumType.STRING)
        private Status status;

        @Column(columnDefinition = "TEXT")
        private String description;

        private LocalDateTime createdAt = LocalDateTime.now();

        public enum TransactionType {
            referral_bonus,
            signup_bonus,
            order_usage,
            admin_adjustment
        }

        public enum Status {
            pending,
            completed,
            cancelled
        }
    }

