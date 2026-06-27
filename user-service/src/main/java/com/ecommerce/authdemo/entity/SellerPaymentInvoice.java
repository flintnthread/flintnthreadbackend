package com.ecommerce.authdemo.entity;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

    @Entity
    @Table(name = "seller_payment_invoices")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class SellerPaymentInvoice {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "order_id", nullable = false)
        private Integer orderId;

        @Column(name = "seller_id", nullable = false)
        private Integer sellerId;

        @Column(name = "invoice_number", nullable = false, length = 50)
        private String invoiceNumber;

        @Column(name = "filename", nullable = false, length = 255)
        private String filename;

        @Column(name = "filepath", nullable = false, length = 500)
        private String filepath;

        @Column(name = "amount", nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Column(name = "created_at", insertable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", insertable = false, updatable = false)
        private LocalDateTime updatedAt;
    }

