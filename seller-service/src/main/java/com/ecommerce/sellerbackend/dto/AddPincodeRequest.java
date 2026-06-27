package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPincodeRequest {

    @NotNull(message = "Area ID is required")
    private Integer areaId;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be a 6-digit number")
    private String pincode;
}
