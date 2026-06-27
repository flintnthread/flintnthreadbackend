package com.ecommerce.sellerbackend.dto.financial;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ShiprocketSyncResponse {
    private String shiprocketOrderId;
    private String shipmentId;
    private String awb;
    private String courier;
    private String status;
    private String trackingUrl;
    private String syncedAt;
    private List<ShiprocketTrackingEventDto> events;
}
