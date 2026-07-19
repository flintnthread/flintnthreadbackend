package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordResetDTO {

    @NotBlank(message = "Email or mobile number is required")
    private String identifier;

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
