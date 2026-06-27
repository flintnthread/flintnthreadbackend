package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellerReviewDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String reviewerName;
    private Integer rating;
    private String comment;
    private String imagePath;
    private LocalDateTime createdAt;
}
