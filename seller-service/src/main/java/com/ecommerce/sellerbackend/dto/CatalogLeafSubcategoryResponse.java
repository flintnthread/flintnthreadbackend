package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CatalogLeafSubcategoryResponse {
    private Integer id;
    private String name;
    private BigDecimal gstPercentage;
    private List<CatalogMaterialOptionResponse> materials;
}
