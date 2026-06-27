package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterSellerRequest {

    @NotBlank(message = "Mobile verification token is required")
    private String mobileVerificationToken;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "First name is required")
    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
