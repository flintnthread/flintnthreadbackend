package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDeliveryCheckResponse {
    private Long productId;
    private String pincode;
    private boolean deliverable;
    private boolean deliverAllLocations;
    private String message;
}
