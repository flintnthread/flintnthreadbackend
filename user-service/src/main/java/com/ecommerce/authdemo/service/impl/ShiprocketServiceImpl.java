package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.OrderTrackingDTO;
import com.ecommerce.authdemo.dto.OrderTrackingResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.service.OrderService;
import com.ecommerce.authdemo.service.ShiprocketService;

import lombok.RequiredArgsConstructor;

import org.cloudinary.json.JSONArray;
import org.cloudinary.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

    @Service
    @RequiredArgsConstructor
    public class ShiprocketServiceImpl
            implements ShiprocketService {

        private static final Logger log =
                LoggerFactory.getLogger(
                        ShiprocketServiceImpl.class
                );

        private final RestTemplate restTemplate;

        private final OrderItemRepository orderItemRepository;

        private final SellerRepository sellerRepository;

        private final OrderRepository orderRepository;

        @Value("${shiprocket.email}")
        private String email;

        @Value("${shiprocket.password}")
        private String password;

        @Value("${shiprocket.api.base-url}")
        private String apiBaseUrl;

        @Value("${shiprocket.pickup-location}")
        private String pickupLocation;

        @Override
        public String getToken() {

            try {

                String url =
                        apiBaseUrl +
                                "/v1/external/auth/login";

                Map<String, String> body =
                        Map.of(
                                "email", email,
                                "password", password
                        );

                ResponseEntity<Map> response =
                        restTemplate.postForEntity(
                                url,
                                body,
                                Map.class
                        );

                if (response.getBody() != null
                        && response.getBody()
                        .containsKey("token")) {

                    return (String)
                            response.getBody()
                                    .get("token");
                }

                throw new RuntimeException(
                        "Shiprocket token failed"
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Shiprocket auth failed",
                        e
                );
            }
        }

        @Override
        public ShiprocketShipmentResult createShipment(
                Order order
        ) {

            try {

                String token = getToken();

                String url =
                        apiBaseUrl +
                                "/v1/external/orders/create/adhoc";

                HttpHeaders headers =
                        new HttpHeaders();

                headers.setBearerAuth(token);

                headers.setContentType(
                        MediaType.APPLICATION_JSON
                );

                Map<String, Object> payload =
                        buildShipmentPayload(order);

                HttpEntity<Map<String, Object>>
                        request =
                        new HttpEntity<>(
                                payload,
                                headers
                        );
                log.info(
                        "Shiprocket Payload: {}",
                        payload
                );

                ResponseEntity<Map> response =
                        restTemplate.postForEntity(
                                url,
                                request,
                                Map.class
                        );

                Map<String, Object> body =
                        response.getBody();

                if (body == null) {

                    throw new RuntimeException(
                            "Empty Shiprocket response"
                    );
                }
                log.info(
                        "Shiprocket Response: {}",
                        body
                );
                String shipmentId = null;

                String shiprocketOrderId = null;

// =====================================
// SHIPROCKET ORDER ID PARSING
// =====================================

                if (body.containsKey("order_id")) {

                    Object orderObj =
                            body.get("order_id");

                    shiprocketOrderId =
                            String.valueOf(orderObj);
                }

// =====================================
// SHIPMENT ID PARSING
// =====================================

                if (body.containsKey("shipment_id")) {

                    Object shipmentObj =
                            body.get("shipment_id");

                    if (shipmentObj instanceof List<?>) {

                        List<?> shipmentList =
                                (List<?>) shipmentObj;

                        if (!shipmentList.isEmpty()) {

                            shipmentId =
                                    String.valueOf(
                                            shipmentList.get(0)
                                    );
                        }

                    } else {

                        shipmentId =
                                String.valueOf(shipmentObj);
                    }
                }

// =====================================
// FALLBACK SHIPMENT IDS
// =====================================

                if ((shipmentId == null || shipmentId.isBlank())
                        && body.containsKey("shipment_ids")) {

                    Object idsObj =
                            body.get("shipment_ids");

                    if (idsObj instanceof List<?>) {

                        List<?> ids =
                                (List<?>) idsObj;

                        if (!ids.isEmpty()) {

                            shipmentId =
                                    String.valueOf(ids.get(0));
                        }
                    }
                }

// =====================================
// SAVE SHIPROCKET IDS
// =====================================

                order.setShiprocketOrderId(
                        shiprocketOrderId
                );

                order.setShiprocketShipmentId(
                        shipmentId
                );

                log.info(
                        "Shiprocket IDs saved orderNumber={} orderId={} shipmentId={}",
                        order.getOrderNumber(),
                        shiprocketOrderId,
                        shipmentId
                );

                order.setShiprocketPushedAt(
                        java.time.LocalDateTime.now()
                );

                order.setShiprocketSyncedAt(
                        java.time.LocalDateTime.now()
                );

                String awb =
                        body.get("awb_code") != null
                                ? body.get("awb_code")
                                .toString()
                                : null;

                String trackingUrl =
                        awb != null
                                ? "https://shiprocket.co/tracking/"
                                + awb
                                : null;

                orderRepository.updateShipment(
                        order.getOrderNumber(),
                        awb,
                        "Shiprocket",
                        trackingUrl,
                        "awb_assigned"
                );

                order.setShiprocketAwbCode(awb);

                order.setShiprocketCourierName(
                        "Shiprocket"
                );

                order.setShiprocketTrackingUrl(
                        trackingUrl
                );

                order.setShiprocketStatus(
                        "awb_assigned"
                );

                orderRepository.save(order);

                return ShiprocketShipmentResult
                        .builder()
                        .shipmentId(shipmentId)
                        .awbCode(awb)
                        .trackingUrl(trackingUrl)
                        .courierName("Shiprocket")
                        .build();

            } catch (
                    HttpClientErrorException
                    | HttpServerErrorException e
            ) {

                throw new RuntimeException(
                        "Shiprocket API error",
                        e
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Shipment creation failed",
                        e
                );
            }
        }

        private Map<String, Object>
        buildShipmentPayload(Order order) {

            Map<String, Object> payload =
                    new HashMap<>();

            List<OrderItem> items =
                    orderItemRepository.findByOrderId(
                            order.getId()
                    );

            List<Map<String, Object>>
                    orderItems =
                    new ArrayList<>();

            double totalWeight = 0;

            double maxLength = 1;
            double maxWidth = 1;
            double maxHeight = 1;

            for (OrderItem item : items) {

                Map<String, Object> line =
                        new HashMap<>();

                line.put(
                        "name",
                        item.getProductName() != null
                                ? item.getProductName()
                                : "Product"
                );

                line.put(
                        "sku",
                        item.getSku() != null
                                ? item.getSku()
                                : "SKU-" + item.getProductId()
                );

                line.put(
                        "units",
                        item.getQuantity()
                );

                line.put(
                        "selling_price",
                        item.getPrice()
                );

                line.put(
                        "hsn",
                        item.getHsnCode() != null
                                ? item.getHsnCode()
                                : "0000"
                );

                orderItems.add(line);

                if (item.getChargeableWeight()
                        != null) {

                    totalWeight +=
                            item.getChargeableWeight();
                }

                if (item.getLengthCm() != null) {

                    maxLength =
                            Math.max(
                                    maxLength,
                                    item.getLengthCm()
                            );
                }

                if (item.getWidthCm() != null) {

                    maxWidth =
                            Math.max(
                                    maxWidth,
                                    item.getWidthCm()
                            );
                }

                if (item.getHeightCm() != null) {

                    maxHeight =
                            Math.max(
                                    maxHeight,
                                    item.getHeightCm()
                            );
                }
            }

            payload.put(
                    "order_id",
                    order.getOrderNumber()
            );

            payload.put(
                    "order_date",
                    LocalDate.now().toString()
            );

            String pickupName = pickupLocation;

            if (!items.isEmpty()
                    && items.get(0).getSellerId() != null) {

                Optional<Seller> sellerOpt =
                        sellerRepository.findById(
                                items.get(0).getSellerId()
                        );

                if (sellerOpt.isPresent()) {

                    Seller seller = sellerOpt.get();

                    if (seller.getBusinessName() != null
                            && !seller.getBusinessName().isBlank()) {

                        pickupName =
                                seller.getBusinessName();
                    }
                }
            }

            payload.put(
                    "pickup_location",
                    pickupName
            );

            // Split customer name into first and last name for Shiprocket
            String fullName = order.getShippingName();
            String firstName = fullName;
            String lastName = "Customer"; // Default last name if not provided
            
            if (fullName != null && fullName.trim().contains(" ")) {
                String[] nameParts = fullName.trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "Customer";
            }
            
            payload.put(
                    "billing_first_name",
                    firstName
            );
            
            payload.put(
                    "billing_last_name",
                    lastName
            );
            
            payload.put(
                    "billing_customer_name",
                    fullName
            );

            payload.put(
                    "billing_phone",
                    order.getShippingPhone()
            );

            payload.put(
                    "billing_email",
                    order.getShippingEmail()
            );

            String billingAddress =
                    order.getShippingAddress1();

            if (order.getShippingAddress2() != null
                    && !order.getShippingAddress2().isBlank()) {

                billingAddress +=
                        ", " + order.getShippingAddress2();
            }

            payload.put(
                    "billing_address",
                    billingAddress
            );

            payload.put(
                    "billing_city",
                    order.getShippingCity()
            );

            payload.put(
                    "billing_state",
                    order.getShippingState()
            );

            payload.put(
                    "billing_pincode",
                    order.getShippingPincode()
            );

            payload.put(
                    "billing_country",
                    "India"
            );

            payload.put(
                    "shipping_is_billing",
                    true
            );

            payload.put(
                    "shipping_first_name",
                    firstName
            );
            
            payload.put(
                    "shipping_last_name",
                    lastName
            );
            
            payload.put(
                    "shipping_customer_name",
                    fullName
            );

            payload.put(
                    "shipping_phone",
                    order.getShippingPhone()
            );

            payload.put(
                    "shipping_address",
                    billingAddress
            );


            payload.put(
                    "shipping_city",
                    order.getShippingCity()
            );

            payload.put(
                    "shipping_state",
                    order.getShippingState()
            );

            payload.put(
                    "shipping_pincode",
                    order.getShippingPincode()
            );

            payload.put(
                    "shipping_country",
                    "India"
            );

            payload.put(
                    "order_items",
                    orderItems
            );

            payload.put(
                    "payment_method",
                    isCodPaymentMethod(order.getPaymentMethod())
                            ? "COD"
                            : "Prepaid"
            );

            payload.put(
                    "sub_total",

                    order.getTotalAmount() != null
                            ? order.getTotalAmount()
                            : 0
            );

            payload.put(
                    "shipping_charges",

                    order.getShippingAmount() != null
                            ? order.getShippingAmount()
                            : 0
            );

            payload.put(
                    "length",
                    Math.max(maxLength, 1)
            );

            payload.put(
                    "breadth",
                    Math.max(maxWidth, 1)
            );

            payload.put(
                    "height",
                    Math.max(maxHeight, 1)
            );

            payload.put(
                    "weight",
                    totalWeight > 0 ? totalWeight : 0.5
            );

            payload.put(
                    "comment",
                    "FNT Order " + order.getOrderNumber()
            );

            payload.put(
                    "channel_id",
                    ""
            );

            payload.put(
                    "tags",
                    List.of(
                            "FNT",
                            "Marketplace",
                            "AutoShipment"
                    )
            );

            return payload;
        }



        @Override
        public String trackShipment(String awb) {

            try {

                String token = getToken();

                String url =
                        apiBaseUrl +
                                "/v1/external/courier/track/awb/"
                                + awb;

                HttpHeaders headers =
                        new HttpHeaders();

                headers.setBearerAuth(token);

                HttpEntity<Void> request =
                        new HttpEntity<>(headers);


                ResponseEntity<Map> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                request,
                                Map.class
                        );

                return response.getBody() != null
                        ? response.getBody().toString()
                        : "{}";

            } catch (Exception e) {

                throw new RuntimeException(
                        "Tracking failed",
                        e
                );
            }
        }

        @Override
        public void handleWebhook(
                Map<String, Object> webhookData
        ) {

            try {

                if (webhookData.containsKey("awb")
                        && webhookData.containsKey("status")) {

                    String awb =
                            webhookData.get("awb")
                                    .toString();

                    String status =
                            webhookData.get("status")
                                    .toString();

                    log.info(
                            "Shiprocket Webhook AWB: {} Status: {}",
                            awb,
                            status
                    );

                    orderRepository
                            .updateOrderStatusFromWebhook(
                                    awb,
                                    status
                            );
                }

            } catch (Exception e) {

                log.error(
                        "Webhook processing failed",
                        e
                );
            }
        }

        @Override
        public void createReversePickup(
                ReturnOrder returnOrder
        ) {

            log.info(
                    "Creating reverse pickup for return {}",
                    returnOrder.getId()
            );

            // TODO:
            // Shiprocket reverse pickup API integration

        }

        @Override
        public void createExchangePickup(
                ReturnExchange exchange
        ) {

            log.info(
                    "Creating exchange pickup for exchange {}",
                    exchange.getId()
            );

            // TODO:
            // Shiprocket exchange pickup API integration

        }

        @Override
        public OrderTrackingResponseDTO
        getTrackingDetails(
                String awb
        ) {

            try {

                String token = getToken();

                String url =
                        apiBaseUrl
                                + "/v1/external/courier/track/awb/"
                                + awb;

                HttpHeaders headers =
                        new HttpHeaders();

                headers.setBearerAuth(token);

                HttpEntity<Void> request =
                        new HttpEntity<>(headers);

                ResponseEntity<Map> response =
                        restTemplate.exchange(

                                url,

                                HttpMethod.GET,

                                request,

                                Map.class
                        );

                Map<String, Object> body =
                        response.getBody();

                if (body == null) {

                    throw new RuntimeException(
                            "Tracking response empty"
                    );
                }

                List<OrderTrackingDTO> timeline =
                        new ArrayList<>();

                try {

                    Map data =
                            (Map) body.get("tracking_data");

                    List<Map<String, Object>> activities =
                            (List<Map<String, Object>>)
                                    data.get("shipment_track_activities");

                    if (activities != null) {

                        for (Map<String, Object> act
                                : activities) {

                            timeline.add(

                                    OrderTrackingDTO
                                            .builder()

                                            .status(
                                                    act.get("activity")
                                                            != null
                                                            ? act.get("activity")
                                                            .toString()
                                                            : ""
                                            )

                                            .description(
                                                    act.get("activity")
                                                            != null
                                                            ? act.get("activity")
                                                            .toString()
                                                            : ""
                                            )

                                            .location(
                                                    act.get("location")
                                                            != null
                                                            ? act.get("location")
                                                            .toString()
                                                            : ""
                                            )

                                            .timestamp(
                                                    java.time.LocalDateTime.now()
                                            )

                                            .build()
                            );
                        }
                    }

                } catch (Exception ignored) {
                }

                Order order =
                        orderRepository
                                .findByShiprocketAwbCode(
                                        awb
                                )
                                .orElse(null);

                return OrderTrackingResponseDTO
                        .builder()

                        .orderId(
                                order != null
                                        ? order.getId()
                                        : null
                        )

                        .orderNumber(
                                order != null
                                        ? order.getOrderNumber()
                                        : null
                        )

                        .awbCode(awb)

                        .courierName(
                                order != null
                                        ? order.getShiprocketCourierName()
                                        : "Shiprocket"
                        )

                        .trackingUrl(
                                order != null
                                        ? order.getShiprocketTrackingUrl()
                                        : null
                        )

                        .currentStatus(
                                order != null
                                        ? order.getShiprocketStatus()
                                        : "processing"
                        )

                        .timeline(timeline)

                        .build();

            } catch (Exception e) {

                throw new RuntimeException(
                        "Tracking details fetch failed",
                        e
                );
            }
        }


        @Override
        public boolean cancelShipment(
                String shiprocketOrderId
        ) {

            try {

                log.info(
                        "Shiprocket cancellation started orderId={}",
                        shiprocketOrderId
                );

                if (shiprocketOrderId == null
                        || shiprocketOrderId.isBlank()) {

                    throw new RuntimeException(
                            "Shiprocket Order ID missing"
                    );
                }

                String token = getToken();

                HttpHeaders headers =
                        new HttpHeaders();

                headers.setContentType(
                        MediaType.APPLICATION_JSON
                );

                headers.setBearerAuth(token);

                JSONObject body =
                        new JSONObject();

                JSONArray ids =
                        new JSONArray();

                ids.put(
                        Integer.parseInt(
                                shiprocketOrderId
                        )
                );

                body.put("ids", ids);

                log.info(
                        "Shiprocket cancel payload={}",
                        body
                );

                HttpEntity<String> entity =
                        new HttpEntity<>(
                                body.toString(),
                                headers
                        );

                ResponseEntity<String> response =
                        restTemplate.exchange(
                                apiBaseUrl
                                        + "/v1/external/orders/cancel",
                                HttpMethod.POST,
                                entity,
                                String.class
                        );

                log.info(
                        "Shiprocket cancel response={}",
                        response.getBody()
                );

                return response.getStatusCode()
                        .is2xxSuccessful();

            } catch (HttpClientErrorException ex) {

                log.error(
                        "Shiprocket cancel API error={}",
                        ex.getResponseBodyAsString(),
                        ex
                );

                return false;

            } catch (Exception ex) {

                log.error(
                        "Shiprocket cancellation exception",
                        ex
                );

                return false;
            }
        }

        private boolean isCodPaymentMethod(String paymentMethod) {
            if (paymentMethod == null || paymentMethod.isBlank()) {
                return false;
            }
            String pm = paymentMethod.trim().toLowerCase();
            return pm.contains("cod")
                    || pm.contains("cash")
                    || pm.equals("cash_on_delivery");
        }
    }



