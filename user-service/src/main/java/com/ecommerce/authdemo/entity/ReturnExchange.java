package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

    @Entity
    @Table(name = "return_exchange")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ReturnExchange {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "order_id")
        private Long orderId;

        @Column(name = "order_item_id")
        private Long orderItemId;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "product_id")
        private Long productId;

        @Column(name = "variant_id")
        private Long variantId;

        @Column(name = "reason")
        private String reason;

        @Column(name = "description")
        private String description;

        @Column(name = "exchange_color")
        private Long exchangeColor;

        @Column(name = "exchange_size")
        private Long exchangeSize;

        @Column(name = "status")
        private String status;

        @Column(name = "admin_comment")
        private String adminComment;

        @Column(name = "processed_by")
        private Long processedBy;

        @Column(name = "processed_at")
        private LocalDateTime processedAt;

        @Column(name = "shiprocket_order_id")
        private String shiprocketOrderId;

        @Column(name = "shiprocket_shipment_id")
        private String shiprocketShipmentId;

        @Column(name = "shiprocket_awb_code")
        private String shiprocketAwbCode;

        @Column(name = "tracking_number")
        private String trackingNumber;

        @Column(name = "shipping_provider")
        private String shippingProvider;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @PrePersist
        public void prePersist() {

            this.createdAt = LocalDateTime.now();

            if (this.status == null) {
                this.status = "pending";
            }
        }
    }

