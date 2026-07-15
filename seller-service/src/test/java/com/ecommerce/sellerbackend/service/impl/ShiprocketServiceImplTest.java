package com.ecommerce.sellerbackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShiprocketServiceImplTest {

    @Test
    void shouldExtractTrackingPayloadFromNestedDataObject() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree("""
                {
                  "order_id": "SR-1001",
                  "shipment_id": "SHIP-900",
                  "data": {
                    "tracking_data": {
                      "shipment_status": "Delivered",
                      "track_url": "https://tracking.example/123",
                      "awb": "123",
                      "courier_name": "Shiprocket",
                      "shipment_track_activities": [
                        {
                          "date": "2024-01-01",
                          "activity": "Booked",
                          "location": "Delhi"
                        }
                      ]
                    }
                  }
                }
                """);

        ShiprocketServiceImpl.TrackingSnapshot snapshot = ShiprocketServiceImpl.parseTrackingSnapshot(response);

        assertEquals("Delivered", snapshot.status());
        assertEquals("https://tracking.example/123", snapshot.trackingUrl());
        assertEquals("123", snapshot.awb());
        assertEquals("SR-1001", snapshot.shiprocketOrderId());
        assertEquals("SHIP-900", snapshot.shipmentId());
        assertEquals("Shiprocket", snapshot.courierName());
        assertFalse(snapshot.activities().isEmpty());
        assertEquals("Booked", snapshot.activities().get(0).status());
        assertEquals("Delhi", snapshot.activities().get(0).location());
    }
}
