package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderReturnDto {
    private Integer id;
    private Long orderId;
    private Integer orderItemId;
    private String productName;
    private String reason;
    private String description;
    private String unboxingVideo;
    private String solution;
    private String solutionLabel;
    private String status;
    private String statusLabel;
    private String adminComment;
    private String shiprocketReturnId;
    private String processedAt;
    private String createdAt;
    private String updatedAt;
}
