package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StateRequest {
    @NotBlank(message = "State name is required")
    @Size(max = 100, message = "State name must not exceed 100 characters")
    private String stateName;

    @NotNull(message = "Country ID is required")
    private Integer countryId;

    private Boolean status;
}
