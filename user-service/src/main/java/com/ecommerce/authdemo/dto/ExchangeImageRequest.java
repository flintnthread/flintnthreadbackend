package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeImageRequest {
    @NotNull(message = "exchangeId is required")
    private Integer exchangeId;

    @NotBlank(message = "imagePath is required")
    @Size(max = 255, message = "imagePath must not exceed 255 characters")
    private String imagePath;
}
