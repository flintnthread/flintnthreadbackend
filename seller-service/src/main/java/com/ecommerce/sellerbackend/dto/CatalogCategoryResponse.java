package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CatalogCategoryResponse {
    private Integer id;
    private String name;
    private List<CatalogSubcategoryResponse> subcategories;
}
