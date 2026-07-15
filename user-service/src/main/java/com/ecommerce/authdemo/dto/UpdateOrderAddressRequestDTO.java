package com.ecommerce.authdemo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrderAddressRequestDTO {

    @Valid
    private OrderAddressSectionDTO shipping;

    /** When null, billing is copied from shipping. */
    @Valid
    private OrderAddressSectionDTO billing;

    /** When true (default), billing is set equal to shipping. Ignored if billing is provided. */
    private Boolean billingSameAsShipping;

    @Data
    public static class OrderAddressSectionDTO {
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @Email(message = "Invalid email format")
        @Size(max = 100)
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
        private String phone;

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        private String addressLine1;

        @Size(max = 255)
        private String addressLine2;

        @NotBlank(message = "City is required")
        @Size(max = 100)
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100)
        private String state;

        @Size(max = 100)
        private String country;

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
        private String pincode;
    }
}
