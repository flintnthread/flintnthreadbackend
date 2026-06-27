package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String customerName;
    private String customerAvatar;
    private int rating;
    private String title;
    private String description;
    private String date;
    private boolean verified;
    private String imageUrl;
    private String sellerReply;
}
