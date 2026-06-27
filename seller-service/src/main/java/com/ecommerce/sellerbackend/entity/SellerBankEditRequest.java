package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_bank_edit_requests")
@Getter
@Setter
public class SellerBankEditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "old_bank_name", length = 100)
    private String oldBankName;

    @Column(name = "old_account_number", length = 50)
    private String oldAccountNumber;

    @Column(name = "old_ifsc_code", length = 20)
    private String oldIfscCode;

    @Column(name = "old_account_holder", length = 100)
    private String oldAccountHolder;

    @Column(name = "old_branch_name", length = 100)
    private String oldBranchName;

    @Column(name = "new_bank_name", nullable = false, length = 100)
    private String newBankName;

    @Column(name = "new_account_number", nullable = false, length = 50)
    private String newAccountNumber;

    @Column(name = "new_ifsc_code", nullable = false, length = 20)
    private String newIfscCode;

    @Column(name = "new_account_holder", nullable = false, length = 100)
    private String newAccountHolder;

    @Column(name = "new_branch_name", length = 100)
    private String newBranchName;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
