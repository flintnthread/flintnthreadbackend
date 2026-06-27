package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeRequest {

    @NotBlank(message = "Size name is required")
    @Size(max = 100, message = "Size name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Size code is required")
    @Size(max = 20, message = "Size code must be at most 20 characters")
    private String code;

    private boolean active = true;
}
