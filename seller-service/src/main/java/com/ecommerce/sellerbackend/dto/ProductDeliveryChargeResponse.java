package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDeliveryChargeResponse {
    private String zone;
    private String standard;
    private String express;
}
