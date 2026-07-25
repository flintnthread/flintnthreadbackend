package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterVerifyRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile number must be a valid 10-digit Indian number"
    )
    private String mobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\d{6}$", message = "Mobile OTP must be 6 digits")
    private String mobileOtp;

    @Pattern(regexp = "^\\d{6}$", message = "Email OTP must be 6 digits")
    private String emailOtp;

    private String referralCode;
}
