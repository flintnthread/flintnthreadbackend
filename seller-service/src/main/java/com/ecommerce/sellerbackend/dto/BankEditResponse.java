package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankEditResponse {

    private Long id;
    private Long sellerId;
    private String oldBankName;
    private String oldAccountNumber;
    private String oldIfscCode;
    private String oldAccountHolder;
    private String oldBranchName;
    private String newBankName;
    private String newAccountNumber;
    private String newIfscCode;
    private String newAccountHolder;
    private String newBranchName;
    private String reason;
    private String status;
    private String adminNote;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private Long approvedByAdminId;
}
