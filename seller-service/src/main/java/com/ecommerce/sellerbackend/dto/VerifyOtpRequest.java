package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 6, message = "OTP must be 4–6 digits")
    private String otp;
}
