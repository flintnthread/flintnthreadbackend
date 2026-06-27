package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderEmailLogDto {
    private Integer id;
    private Long orderId;
    private String email;
    private String emailType;
    private String subject;
    private String status;
    private String sentAt;
    private String errorMessage;
}
