package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaqRequest {
    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    private Integer sortOrder;
    private Boolean status;
}
