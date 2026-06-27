package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankEditApprovalRequest {

    @NotBlank(message = "Action is required")
    private String action; // "approve" or "reject"

    private String adminNote;
}
