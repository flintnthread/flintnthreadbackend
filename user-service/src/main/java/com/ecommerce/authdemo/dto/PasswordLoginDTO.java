package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordLoginDTO {

    /** Email address or 10-digit Indian mobile number. */
    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 72, message = "Password is required")
    private String password;
}
