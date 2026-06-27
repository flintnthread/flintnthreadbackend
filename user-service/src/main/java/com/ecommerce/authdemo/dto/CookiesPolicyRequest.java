package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CookiesPolicyRequest {
    @NotBlank(message = "Content is required")
    private String content;
}
