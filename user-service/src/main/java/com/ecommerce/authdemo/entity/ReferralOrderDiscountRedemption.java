package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name="referral_order_discount_redemptions")
    @Getter
    @Setter
    public class ReferralOrderDiscountRedemption {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private Long userId;

        @Column(unique = true)
        private Long orderId;


        private BigDecimal discountAmount;

        private BigDecimal cartSubtotal;

        private LocalDateTime createdAt = LocalDateTime.now();
    }

