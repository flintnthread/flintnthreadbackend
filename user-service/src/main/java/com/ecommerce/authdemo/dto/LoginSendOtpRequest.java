package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginSendOtpRequest {

    /** Email address or 10-digit Indian mobile number. */
    @NotBlank(message = "Email or Mobile is required")
    private String identifier;
}
