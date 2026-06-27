package com.ecommerce.authdemo.entity;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "wallet_transactions")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class WalletTransaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "seller_id", nullable = false)
        private Integer userId;

        @Column(name = "order_id")
        private Integer orderId;

        @Column(name = "amount", nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false)
        private Type type;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        @Column(name = "created_by")
        private Integer createdBy;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        // ✅ ENUM
        public enum Type {
            credit,
            debit
        }
    }

