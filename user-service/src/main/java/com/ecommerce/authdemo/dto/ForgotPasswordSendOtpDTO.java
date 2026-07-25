package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordSendOtpDTO {

    /** Email address or 10-digit Indian mobile number. */
    @NotBlank(message = "Email or mobile number is required")
    private String identifier;
}
