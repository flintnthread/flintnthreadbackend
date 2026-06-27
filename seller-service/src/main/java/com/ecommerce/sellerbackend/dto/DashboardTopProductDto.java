package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardTopProductDto {
    private String id;
    private String name;
    private String price;
    private long sold;
    private String image;
    private String category;
}
