package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestPayload {

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must be at most 255 characters")
    private String categoryName;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Reason for request is required")
    @Size(max = 5000, message = "Reason is too long")
    private String reason;
}
