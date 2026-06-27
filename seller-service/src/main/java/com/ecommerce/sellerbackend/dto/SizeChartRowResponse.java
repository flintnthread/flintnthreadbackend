package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SizeChartRowResponse {
    private String size;
    private String chest;
    private String waist;
    private String hip;
    private String length;
    private String sleeve;
}
