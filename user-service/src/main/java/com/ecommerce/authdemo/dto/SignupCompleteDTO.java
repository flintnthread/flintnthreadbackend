package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupCompleteDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 40, message = "First name must be 1–40 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 40, message = "Last name must be 1–40 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email verification token is required")
    private String emailVerifiedToken;

    /** Optional when phone was verified with a separate phoneVerifiedToken. */
    private String phoneVerifiedToken;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;

    private String referralCode;
}
