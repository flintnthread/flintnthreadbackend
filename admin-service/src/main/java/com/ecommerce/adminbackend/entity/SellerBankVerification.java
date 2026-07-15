package com.ecommerce.adminbackend.entity;

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
@Table(name = "seller_bank_verifications")
@Getter
@Setter
public class SellerBankVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false)
    private String ifscCode;

    @Column(name = "account_holder", nullable = false)
    private String accountHolder;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "verification_amount", precision = 10, scale = 2)
    private BigDecimal verificationAmount;

    @Column(name = "reference_number", nullable = false)
    private String referenceNumber;

    @Column(name = "utr_number")
    private String utrNumber;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "fund_account_id")
    private String fundAccountId;

    @Column(name = "payout_id")
    private String payoutId;

    @Column(name = "status")
    private String status;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
