package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginVerifyRequest {

    /** Email address or 10-digit Indian mobile number. */
    @NotBlank(message = "Email or Mobile is required")
    private String identifier;

    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;
}
