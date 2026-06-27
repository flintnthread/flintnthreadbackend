package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductPackagingResponse {
    private String boxDimensions;
    private String grossWeight;
    private String packagingType;
    private boolean fragile;
}
