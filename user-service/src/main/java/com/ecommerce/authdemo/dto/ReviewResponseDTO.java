package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponseDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private String name;
    private String email;
    private Integer rating;
    private String comment;
    private String imagePath;
    private Boolean status;
    private LocalDateTime createdAt;
}
