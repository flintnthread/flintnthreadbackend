package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityReviewDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer rating;
    private String comment;
    private String imagePath;
    private Boolean status;
    private LocalDateTime createdAt;
}
