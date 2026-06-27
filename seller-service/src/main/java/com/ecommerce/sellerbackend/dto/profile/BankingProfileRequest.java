package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankingProfileRequest {

    @NotBlank(message = "IFSC code is required")
    private String ifscCode;

    private String bankName;

    private String branchName;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;
}
