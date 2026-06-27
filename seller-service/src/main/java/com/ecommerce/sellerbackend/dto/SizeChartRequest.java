package com.ecommerce.sellerbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SizeChartRequest {

    @NotBlank
    private String name;

    private Integer categoryId;
    private Integer subcategoryId;
    private String categoryName;
    private String categorySubName;
    private String subcategoryName;
    private String unit;
    private String notes;
    /** Base64 data URL, https URL, or existing uploads/ path */
    private String imageSource;

    @NotEmpty
    @Valid
    private List<SizeChartRowRequest> rows;
}
