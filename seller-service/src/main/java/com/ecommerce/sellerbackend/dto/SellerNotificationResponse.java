package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerNotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String body;
    private String time;
    private boolean read;
}
