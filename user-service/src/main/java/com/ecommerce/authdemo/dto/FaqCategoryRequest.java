package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaqCategoryRequest {
    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    private String categoryName;

    @Size(max = 255, message = "Category icon must not exceed 255 characters")
    private String categoryIcon;

    private Integer sortOrder;
    private Boolean status;
}
