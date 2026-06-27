package com.ecommerce.sellerbackend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyPanRequest {

    @NotBlank(message = "Company PAN number is required")
    private String companyPanNumber;
}
