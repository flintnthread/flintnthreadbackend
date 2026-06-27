package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankEditRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    private String ifscCode;

    @NotBlank(message = "Account holder is required")
    private String accountHolder;

    private String branchName;

    @NotBlank(message = "Reason is required")
    private String reason;
}
