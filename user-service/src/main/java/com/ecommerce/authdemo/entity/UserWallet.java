package com.ecommerce.authdemo.entity;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "user_wallet")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class UserWallet {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "user_id", nullable = false, unique = true)
        private Integer userId;

        @Column(name = "balance", precision = 10, scale = 2)
        private BigDecimal balance;

        @Column(name = "total_earned", precision = 10, scale = 2)
        private BigDecimal totalEarned;

        @Column(name = "total_spent", precision = 10, scale = 2)
        private BigDecimal totalSpent;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", insertable = false, updatable = false)
        private LocalDateTime updatedAt;
    }

