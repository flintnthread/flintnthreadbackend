package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{10,15}$", message = "Enter a valid mobile number")
    private String mobile;

    private String method = "sms";
}
