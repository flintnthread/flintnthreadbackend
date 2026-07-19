package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpDTO {

    private String email;

    /** Optional when verifying email login that already has a linked phone. */
    @Pattern(
            regexp = "^$|^[6-9]\\d{9}$",
            message = "Mobile number must be a valid 10-digit Indian number"
    )
    private String mobile;

    @Pattern(
            regexp = "^\\d{6}$",
            message = "OTP must be 6 digits"
    )
    private String otp;

    private String referralCode;
}