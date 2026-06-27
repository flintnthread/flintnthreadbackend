package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketShipmentResult {

    private String shipmentId;
    private String awbCode;
    private String trackingUrl;
    private String courierName;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("shipment_id", shipmentId);
        m.put("awb_code", awbCode);
        m.put("tracking_url", trackingUrl);
        m.put("courier_name", courierName);
        return m;
    }
}
