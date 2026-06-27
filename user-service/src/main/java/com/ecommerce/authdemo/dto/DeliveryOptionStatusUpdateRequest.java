package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryOptionStatusUpdateRequest {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
