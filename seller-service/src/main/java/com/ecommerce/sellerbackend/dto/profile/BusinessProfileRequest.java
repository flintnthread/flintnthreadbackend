package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessProfileRequest {

    @NotBlank(message = "Business category is required")
    @Pattern(regexp = "(?i)^(b2b|b2c)$", message = "Business category must be b2b or b2c")
    private String businessCategory;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Business address is required")
    private String address;

    private Boolean hasGst;

    private String gstType;

    private String gstNumber;

    private Boolean gstVerified;

    @NotBlank(message = "PAN number is required")
    private String panNumber;

    /** Optional when seller already has Aadhaar on file (masked value sent from client). */
    private String aadhaarNumber;
}
