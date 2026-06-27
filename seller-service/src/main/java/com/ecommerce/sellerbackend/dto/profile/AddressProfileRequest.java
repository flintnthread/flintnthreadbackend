package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressProfileRequest {

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "Landmark is required")
    private String landmark;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Area is required")
    private String area;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private Boolean warehouseDifferent;

    private String warehouseAddress;
    private String warehouseLandmark;
    private String warehouseCity;
    private String warehouseState;
    private String warehouseArea;
    private String warehouseCountry;
    private String warehousePincode;
}
