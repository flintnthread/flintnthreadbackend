package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaqCategoryStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private Boolean status;
}
