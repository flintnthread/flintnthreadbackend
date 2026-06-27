package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOptionResponse {
    private Integer id;
    private Integer sellerId;
    private String optionName;
    private Integer minDays;
    private Integer maxDays;
    private String deliveryInfo;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
