package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile number must be a valid 10-digit Indian number"
    )
    private String mobile;
}