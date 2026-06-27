package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PincodeRequest {
    @NotNull(message = "Country ID is required")
    private Integer countryId;

    @NotNull(message = "State ID is required")
    private Integer stateId;

    @NotNull(message = "City ID is required")
    private Integer cityId;

    @NotNull(message = "Area ID is required")
    private Integer areaId;

    @NotBlank(message = "Pincode is required")
    @Size(max = 10, message = "Pincode must not exceed 10 characters")
    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "Invalid pincode format")
    private String pincode;

    private Boolean status;
}
