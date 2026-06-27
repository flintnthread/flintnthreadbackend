package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CountryRequest {

    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String countryName;

    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be exactly 2 letters")
    private String countryCode;

    private Boolean status;
}
