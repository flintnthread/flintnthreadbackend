package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLocationRequest {

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Area is required")
    private String area;

    @Pattern(regexp = "^$|^\\d{6}$", message = "Pincode must be a 6-digit number")
    private String pincode;
}
