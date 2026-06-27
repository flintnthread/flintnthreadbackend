package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

    @Entity
    @Table(name = "payments")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public class Payment extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private Long orderId;

        private String paymentGateway;

        private String paymentTransactionId;

        private BigDecimal amount;

        private String currency;

        private String status;
    }

