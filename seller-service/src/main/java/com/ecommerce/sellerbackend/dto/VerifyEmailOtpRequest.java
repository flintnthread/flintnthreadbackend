package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 6, message = "OTP must be 4–6 digits")
    private String otp;
}
