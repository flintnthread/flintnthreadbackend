package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GstVerifyRequest {

    @NotBlank(message = "GST number is required")
    private String gstNumber;
}
