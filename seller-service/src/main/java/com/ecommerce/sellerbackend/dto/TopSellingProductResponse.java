package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopSellingProductResponse {
    private String id;
    private String name;
    private String price;
    private long sold;
    private String image;
    private String category;
    private String status;
    /** Average star rating from approved reviews; null if none */
    private Double avgRating;
    /** Formatted MRP when variant has mrp_price */
    private String mrp;
    /** Display discount e.g. "15% OFF" when variant has discount */
    private String discount;
}
