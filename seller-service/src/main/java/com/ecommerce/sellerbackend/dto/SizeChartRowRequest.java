package com.ecommerce.sellerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeChartRowRequest {

    @NotBlank
    private String size;

    private String chest;
    private String waist;
    private String hip;
    private String length;
    private String sleeve;
}
