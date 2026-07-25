package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.OrderTrackingResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.entity.ReturnExchange;
import com.ecommerce.authdemo.entity.ReturnOrder;

import java.util.Map;

public interface ShiprocketService {

    String getToken();

    ShiprocketShipmentResult createShipment(Order order);

    /**
     * Pull latest AWB / courier / tracking URL from Shiprocket into {@code orders}
     * (used when Ship Now happens in the Shiprocket dashboard and webhook did not update DB).
     */
    ShiprocketShipmentResult syncShipmentDetails(Order order);

    String trackShipment(String awb);

    void handleWebhook(Map<String, Object> webhookData);

    void createReversePickup(
            ReturnOrder returnOrder
    );

    void createExchangePickup(
            ReturnExchange exchange
    );

    OrderTrackingResponseDTO
    getTrackingDetails(
            String awb
    );


    boolean cancelShipment(String shiprocketOrderId);

}