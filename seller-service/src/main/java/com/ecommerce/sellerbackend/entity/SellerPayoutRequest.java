package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_payout_requests")
@Getter
@Setter
public class SellerPayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "seller_note", length = 500)
    private String sellerNote;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
