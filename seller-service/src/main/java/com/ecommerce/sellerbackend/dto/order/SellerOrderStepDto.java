package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerOrderStepDto {
    private String key;
    private String label;
    private String date;
    private String iconLib;
    private String iconName;
    private String status;
    private String comment;
}
