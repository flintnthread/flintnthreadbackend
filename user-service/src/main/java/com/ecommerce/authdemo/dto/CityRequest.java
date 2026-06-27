package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CityRequest {

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "State ID is required")
    private Integer stateId;

    @NotNull(message = "Country ID is required")
    private Integer countryId;

    private Boolean status;
}
