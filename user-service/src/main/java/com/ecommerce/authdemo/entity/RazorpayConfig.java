package com.ecommerce.authdemo.entity;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "razorpay_config")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class RazorpayConfig {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "key_id", nullable = false, length = 100)
        private String keyId;

        @Column(name = "key_secret", nullable = false, length = 100)
        private String keySecret;

        @Column(name = "account_number", length = 50)
        private String accountNumber;

        @Column(name = "contact_id", length = 100)
        private String contactId;

        @Enumerated(EnumType.STRING)
        @Column(name = "mode")
        private Mode mode;

        @Column(name = "webhook_secret", length = 100)
        private String webhookSecret;

        @Column(name = "payout_enabled")
        private Boolean payoutEnabled;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", insertable = false, updatable = false)
        private LocalDateTime updatedAt;

        public enum Mode {
            test,
            live
        }
    }

