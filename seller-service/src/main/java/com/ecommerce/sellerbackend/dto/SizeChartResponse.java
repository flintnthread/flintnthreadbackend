package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SizeChartResponse {
    private Integer id;
    private String name;
    private Integer categoryId;
    private Integer subcategoryId;
    private String categoryName;
    private String categorySubName;
    private String subcategoryName;
    private String unit;
    private String notes;
    private String imageUrl;
    private List<SizeChartRowResponse> rows;
}
