package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryOptionRequest {

    private Integer sellerId;

    @NotBlank(message = "Option name is required")
    @Size(max = 255, message = "Option name must not exceed 255 characters")
    private String optionName;

    @NotNull(message = "minDays is required")
    @Min(value = 0, message = "minDays must be >= 0")
    private Integer minDays;

    @NotNull(message = "maxDays is required")
    @Min(value = 0, message = "maxDays must be >= 0")
    private Integer maxDays;

    private String deliveryInfo;
    private Boolean isActive;
}
