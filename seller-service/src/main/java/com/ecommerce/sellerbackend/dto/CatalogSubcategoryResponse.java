package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CatalogSubcategoryResponse {
    private Integer id;
    private String name;
    private BigDecimal gstPercentage;
    private List<CatalogMaterialOptionResponse> materials;
    /** Leaf sub-types under this category row (e.g. Folding chairs under Chairs). */
    private List<CatalogLeafSubcategoryResponse> children;
}
