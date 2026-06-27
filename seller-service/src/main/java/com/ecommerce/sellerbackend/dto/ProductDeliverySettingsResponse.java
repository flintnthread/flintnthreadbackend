package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductDeliverySettingsResponse {
    private Long productId;
    private boolean deliverAllLocations;
    private List<PincodeOptionResponse> pincodes;
}
