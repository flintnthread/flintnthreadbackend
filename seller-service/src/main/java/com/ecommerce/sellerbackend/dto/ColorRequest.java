package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorRequest {

    @NotBlank(message = "Color name is required")
    @Size(max = 100, message = "Color name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Color code is required")
    @Size(max = 20, message = "Color code must be at most 20 characters")
    private String hex;

    private boolean active = true;
}
