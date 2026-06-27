package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDeliveryInfoResponse {
    private String estimated;
    private String freeAbove;
    private boolean expressAvailable;
    private String expressCharge;
    private boolean cod;
    private String codCharge;
    private String locations;
}
