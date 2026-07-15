package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.OrderTrackingDTO;
import com.ecommerce.authdemo.dto.OrderTrackingResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.service.PlatformIntegrationSettings;
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

        private final OrderRepository orderRepository;

        private final ProductRepository productRepository;

        private final ProductVariantRepository productVariantRepository;

        private final PlatformIntegrationSettings integrationSettings;

        @Value("${shiprocket.api.base-url}")
        private String apiBaseUrl;

        /** Optional map: sellerId:PickupNickname,sellerId:AnotherNickname */
        @Value("${shiprocket.pickup-location-by-seller:}")
        private String pickupLocationBySeller;

        @Override
        public String getToken() {

            try {
                String email = integrationSettings.getShiprocketEmail();
                String password = integrationSettings.getShiprocketPassword();
                if (email == null || email.isBlank()
                        || password == null || password.isBlank()) {
                    throw new RuntimeException(
                            "Shiprocket credentials missing. Set them in Admin → Platform Settings."
                    );
                }

                String url = apiBaseUrl + "/auth/login";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                Map<String, String> body = new LinkedHashMap<>();
                body.put("email", email.trim());
                body.put("password", password);

                HttpEntity<Map<String, String>> request =
                        new HttpEntity<>(body, headers);

                ResponseEntity<Map> response =
                        restTemplate.postForEntity(url, request, Map.class);

                if (response.getBody() != null
                        && response.getBody().containsKey("token")) {
                    return (String) response.getBody().get("token");
                }

                throw new RuntimeException(
                        "Shiprocket token failed: " + response.getBody()
                );

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                String apiBody = e.getResponseBodyAsString();
                log.error(
                        "Shiprocket auth API error status={} body={}",
                        e.getStatusCode(),
                        apiBody
                );
                throw new RuntimeException(
                        "Shiprocket auth failed: "
                                + (apiBody != null && !apiBody.isBlank() ? apiBody : e.getMessage()),
                        e
                );
            } catch (Exception e) {
                log.error("Shiprocket auth failed: {}", e.getMessage(), e);
                throw new RuntimeException(
                        "Shiprocket auth failed: " + e.getMessage(),
                        e
                );
            }
        }

        @Override
        public ShiprocketShipmentResult createShipment(
                Order order
        ) {

            if (order == null || order.getId() == null) {
                throw new IllegalArgumentException("Order is required for Shiprocket shipment.");
            }

            // Idempotent: never create a duplicate Shiprocket order for the same FNT order.
            if (order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank()) {
                log.info(
                        "Shiprocket already linked for orderNumber={} shiprocketOrderId={} — skipping create",
                        order.getOrderNumber(),
                        order.getShiprocketOrderId()
                );
                return ShiprocketShipmentResult.builder()
                        .shipmentId(order.getShiprocketShipmentId())
                        .awbCode(order.getShiprocketAwbCode())
                        .trackingUrl(order.getShiprocketTrackingUrl())
                        .courierName(order.getShiprocketCourierName() != null
                                ? order.getShiprocketCourierName()
                                : "Shiprocket")
                        .build();
            }

            try {
                Map<String, Object> payload = buildShipmentPayload(order);
                Map<String, Object> body = postCreateAdhocWithPickupFallback(order, payload);
                return persistShiprocketCreateResponse(order, body);
            } catch (
                    HttpClientErrorException
                    | HttpServerErrorException e
            ) {
                String apiBody = e.getResponseBodyAsString();
                log.error(
                        "Shiprocket API error orderNumber={} body={}",
                        order.getOrderNumber(),
                        apiBody,
                        e
                );
                throw new RuntimeException(
                        "Shiprocket API error: " + (apiBody != null && !apiBody.isBlank() ? apiBody : e.getMessage()),
                        e
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Shipment creation failed: " + e.getMessage(),
                        e
                );
            }
        }

        /**
         * Create on Shiprocket. If pickup nickname is wrong, retry once with configured default (work).
         */
        private Map<String, Object> postCreateAdhocWithPickupFallback(
                Order order,
                Map<String, Object> payload
        ) {
            try {
                return postCreateAdhoc(order, payload);
            } catch (RuntimeException first) {
                String configured = defaultPickupLocation();
                Object usedPickup = payload.get("pickup_location");
                String used = usedPickup != null ? String.valueOf(usedPickup) : "";
                String detail = first.getMessage() != null ? first.getMessage() : "";
                if (first instanceof HttpClientErrorException httpEx) {
                    String apiBody = httpEx.getResponseBodyAsString();
                    if (apiBody != null && !apiBody.isBlank()) {
                        detail = detail + " " + apiBody;
                    }
                }
                String msg = detail.toLowerCase();
                boolean pickupIssue = msg.contains("pickup")
                        || msg.contains("location")
                        || msg.contains("warehouse")
                        || msg.contains("invalid pickup");
                boolean phoneIssue = msg.contains("phone");

                if (phoneIssue || !pickupIssue || configured.equalsIgnoreCase(used)) {
                    throw first;
                }

                log.warn(
                        "Shiprocket create failed with pickup={} for orderNumber={} — retrying with pickup={}",
                        used,
                        order.getOrderNumber(),
                        configured
                );
                payload.put("pickup_location", configured);
                return postCreateAdhoc(order, payload);
            }
        }

        private Map<String, Object> postCreateAdhoc(
                Order order,
                Map<String, Object> payload
        ) {
            String token = getToken();
            String url = apiBaseUrl + "/orders/create/adhoc";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            log.info(
                    "Shiprocket Payload orderNumber={} pickup={} phone={} items={}",
                    order.getOrderNumber(),
                    payload.get("pickup_location"),
                    payload.get("billing_phone"),
                    payload.get("order_items") instanceof List<?> list ? list.size() : 0
            );

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty Shiprocket response");
            }
            log.info("Shiprocket Response for {}: {}", order.getOrderNumber(), body);

            Object statusCode = body.get("status_code");
            if (statusCode != null) {
                int code;
                try {
                    code = Integer.parseInt(String.valueOf(statusCode));
                } catch (NumberFormatException ex) {
                    code = -1;
                }
                // status_code 1 = success on Shiprocket create/adhoc
                if (code != 1 && code != 200) {
                    String message = body.get("message") != null
                            ? String.valueOf(body.get("message"))
                            : body.toString();
                    throw new RuntimeException("Shiprocket rejected order: " + message);
                }
            }
            return body;
        }

        private ShiprocketShipmentResult persistShiprocketCreateResponse(
                Order order,
                Map<String, Object> body
        ) {
            String shipmentId = null;
            String shiprocketOrderId = null;

            if (body.containsKey("order_id")) {
                shiprocketOrderId = String.valueOf(body.get("order_id"));
            }

            if (body.containsKey("shipment_id")) {
                Object shipmentObj = body.get("shipment_id");
                if (shipmentObj instanceof List<?> shipmentList) {
                    if (!shipmentList.isEmpty()) {
                        shipmentId = String.valueOf(shipmentList.get(0));
                    }
                } else {
                    shipmentId = String.valueOf(shipmentObj);
                }
            }

            if ((shipmentId == null || shipmentId.isBlank()) && body.containsKey("shipment_ids")) {
                Object idsObj = body.get("shipment_ids");
                if (idsObj instanceof List<?> ids && !ids.isEmpty()) {
                    shipmentId = String.valueOf(ids.get(0));
                }
            }

            if (shiprocketOrderId == null || shiprocketOrderId.isBlank()
                    || "null".equalsIgnoreCase(shiprocketOrderId)) {
                throw new RuntimeException(
                        "Shiprocket did not return order_id. Response: " + body
                );
            }

            order.setShiprocketOrderId(shiprocketOrderId);
            order.setShiprocketShipmentId(shipmentId);
            order.setShiprocketPushedAt(java.time.LocalDateTime.now());
            order.setShiprocketSyncedAt(java.time.LocalDateTime.now());

            log.info(
                    "Shiprocket IDs saved orderNumber={} orderId={} shipmentId={}",
                    order.getOrderNumber(),
                    shiprocketOrderId,
                    shipmentId
            );

            String awb =
                    body.get("awb_code") != null
                            && !"null".equalsIgnoreCase(String.valueOf(body.get("awb_code")))
                            && !String.valueOf(body.get("awb_code")).isBlank()
                            ? String.valueOf(body.get("awb_code"))
                            : null;

            String trackingUrl = awb != null
                    ? "https://shiprocket.co/tracking/" + awb
                    : null;

            String shiprocketStatus = awb != null ? "awb_assigned" : "new";

            order.setShiprocketAwbCode(awb);
            order.setShiprocketCourierName("Shiprocket");
            order.setShiprocketTrackingUrl(trackingUrl);
            order.setShiprocketStatus(shiprocketStatus);

            // Persist Shiprocket IDs first so a later AWB update cannot leave the order unlinked.
            orderRepository.save(order);
            orderRepository.updateShipment(
                    order.getOrderNumber(),
                    awb,
                    "Shiprocket",
                    trackingUrl,
                    shiprocketStatus
            );

            return ShiprocketShipmentResult
                    .builder()
                    .shipmentId(shipmentId)
                    .awbCode(awb)
                    .trackingUrl(trackingUrl)
                    .courierName("Shiprocket")
                    .build();
        }

        private Map<String, Object>
        buildShipmentPayload(Order order) {

            Map<String, Object> payload =
                    new HashMap<>();

            List<OrderItem> items =
                    orderItemRepository.findByOrderId(
                            order.getId()
                    );

            if (items == null || items.isEmpty()) {
                throw new RuntimeException(
                        "No order items found for Shiprocket order " + order.getOrderNumber()
                );
            }

            List<Map<String, Object>>
                    orderItems =
                    new ArrayList<>();

            double totalWeight = 0;

            double maxLength = 1;
            double maxWidth = 1;
            double maxHeight = 1;
            Long primarySellerId = null;

            for (OrderItem item : items) {
                enrichOrderItemFromCatalog(item);

                if (primarySellerId == null && item.getSellerId() != null) {
                    primarySellerId = item.getSellerId();
                }

                Map<String, Object> line =
                        new HashMap<>();

                line.put(
                        "name",
                        item.getProductName() != null && !item.getProductName().isBlank()
                                ? item.getProductName()
                                : "Product"
                );

                line.put(
                        "sku",
                        item.getSku() != null && !item.getSku().isBlank()
                                ? item.getSku()
                                : "SKU-" + item.getProductId()
                );

                line.put(
                        "units",
                        item.getQuantity() != null ? item.getQuantity() : 1
                );

                line.put(
                        "selling_price",
                        item.getPrice() != null ? item.getPrice() : 0
                );

                line.put(
                        "hsn",
                        item.getHsnCode() != null && !item.getHsnCode().isBlank()
                                ? item.getHsnCode()
                                : "0000"
                );

                orderItems.add(line);

                double lineWeight = item.getChargeableWeight() != null
                        ? item.getChargeableWeight()
                        : (item.getWeight() != null ? item.getWeight() : 0.5);
                totalWeight += lineWeight * (item.getQuantity() != null ? item.getQuantity() : 1);

                if (item.getLengthCm() != null) {
                    maxLength = Math.max(maxLength, item.getLengthCm());
                }
                if (item.getWidthCm() != null) {
                    maxWidth = Math.max(maxWidth, item.getWidthCm());
                }
                if (item.getHeightCm() != null) {
                    maxHeight = Math.max(maxHeight, item.getHeightCm());
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

            payload.put(
                    "pickup_location",
                    resolvePickupLocation(primarySellerId)
            );

            // Split customer name into first and last name for Shiprocket
            String fullName = order.getShippingName();
            String firstName = fullName != null && !fullName.isBlank() ? fullName.trim() : "Customer";
            String lastName = "Customer";

            if (fullName != null && fullName.trim().contains(" ")) {
                String[] nameParts = fullName.trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "Customer";
            }

            payload.put("billing_customer_name", firstName);
            payload.put("billing_last_name", lastName);
            // Shiprocket requires a valid 10-digit Indian mobile (6–9…). Invalid phones → HTTP 422.
            payload.put("billing_phone", normalizeIndianMobile(order.getShippingPhone()));
            payload.put("billing_email", order.getShippingEmail() != null ? order.getShippingEmail() : "support@flintnthread.in");

            String billingAddress =
                    order.getShippingAddress1();

            if (order.getShippingAddress2() != null
                    && !order.getShippingAddress2().isBlank()) {

                billingAddress +=
                        ", " + order.getShippingAddress2();
            }
            if (billingAddress == null || billingAddress.isBlank()) {
                billingAddress = "Address not provided";
            }

            payload.put("billing_address", billingAddress);
            payload.put("billing_city",
                    order.getShippingCity() != null && !order.getShippingCity().isBlank()
                            ? order.getShippingCity().trim()
                            : "Hyderabad");
            payload.put("billing_state",
                    order.getShippingState() != null && !order.getShippingState().isBlank()
                            ? order.getShippingState().trim()
                            : "Telangana");
            String pincode = order.getShippingPincode() != null
                    ? order.getShippingPincode().replaceAll("[^0-9]", "")
                    : "";
            if (pincode.length() != 6) {
                throw new RuntimeException(
                        "Invalid shipping pincode for Shiprocket: '" + order.getShippingPincode()
                                + "'. Need a valid 6-digit PIN."
                );
            }
            payload.put("billing_pincode", pincode);
            payload.put("billing_country", "India");
            payload.put("shipping_is_billing", true);

            payload.put("order_items", orderItems);

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

            payload.put("length", Math.max(maxLength, 1));
            payload.put("breadth", Math.max(maxWidth, 1));
            payload.put("height", Math.max(maxHeight, 1));
            payload.put("weight", totalWeight > 0 ? totalWeight : 0.5);

            payload.put(
                    "comment",
                    "FNT Order " + order.getOrderNumber()
            );

            return payload;
        }

        /**
         * Shiprocket pickup_location must match a nickname registered in Shiprocket
         * (Settings → Pickup Addresses), e.g. "ASVI HOME FOODS".
         * Prefer: per-seller map → configured default from Admin Platform Settings.
         */
        private String defaultPickupLocation() {
            String configured = integrationSettings.getShiprocketPickupLocation();
            return configured != null && !configured.isBlank()
                    ? configured.trim()
                    : "ASVI HOME FOODS";
        }

        private String resolvePickupLocation(Long sellerId) {
            String configured = defaultPickupLocation();

            if (sellerId == null || pickupLocationBySeller == null || pickupLocationBySeller.isBlank()) {
                return configured;
            }

            for (String part : pickupLocationBySeller.split(",")) {
                String entry = part.trim();
                if (entry.isEmpty()) {
                    continue;
                }
                int colon = entry.indexOf(':');
                if (colon <= 0 || colon >= entry.length() - 1) {
                    continue;
                }
                String idPart = entry.substring(0, colon).trim();
                String namePart = entry.substring(colon + 1).trim();
                try {
                    if (Long.parseLong(idPart) == sellerId && !namePart.isBlank()) {
                        return namePart;
                    }
                } catch (NumberFormatException ignored) {
                    // skip malformed mapping
                }
            }
            return configured;
        }

        /**
         * Shiprocket India phones: exactly 10 digits starting with 6–9.
         * Strips +91 / 91 / leading 0 and non-digits.
         */
        private String normalizeIndianMobile(String rawPhone) {
            if (rawPhone == null || rawPhone.isBlank()) {
                throw new RuntimeException(
                        "Shipping phone is required for Shiprocket. Update the delivery address phone."
                );
            }
            String digits = rawPhone.replaceAll("[^0-9]", "");
            if (digits.startsWith("91") && digits.length() > 10) {
                digits = digits.substring(digits.length() - 10);
            } else if (digits.startsWith("0") && digits.length() == 11) {
                digits = digits.substring(1);
            } else if (digits.length() > 10) {
                digits = digits.substring(digits.length() - 10);
            }

            if (!digits.matches("^[6-9]\\d{9}$")) {
                throw new RuntimeException(
                        "Invalid shipping phone for Shiprocket: '" + rawPhone
                                + "'. Need a valid 10-digit Indian mobile."
                );
            }
            return digits;
        }

        private void enrichOrderItemFromCatalog(OrderItem item) {
            if (item == null || item.getProductId() == null) {
                return;
            }

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            ProductVariant variant = null;
            if (item.getVariantId() != null) {
                variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
            }

            if ((item.getProductName() == null || item.getProductName().isBlank()) && product != null) {
                item.setProductName(product.getName());
            }
            if ((item.getHsnCode() == null || item.getHsnCode().isBlank()) && product != null) {
                item.setHsnCode(product.getHsnCode());
            }
            if ((item.getSku() == null || item.getSku().isBlank())) {
                if (variant != null && variant.getSku() != null && !variant.getSku().isBlank()) {
                    item.setSku(variant.getSku());
                } else if (product != null && product.getSku() != null) {
                    item.setSku(product.getSku());
                }
            }

            double length = item.getLengthCm() != null
                    ? item.getLengthCm()
                    : (product != null && product.getLengthCm() != null ? product.getLengthCm().doubleValue() : 1.0);
            double width = item.getWidthCm() != null
                    ? item.getWidthCm()
                    : (product != null && product.getWidthCm() != null ? product.getWidthCm().doubleValue() : 1.0);
            double height = item.getHeightCm() != null
                    ? item.getHeightCm()
                    : (product != null && product.getHeightCm() != null ? product.getHeightCm().doubleValue() : 1.0);
            double weight = item.getWeight() != null
                    ? item.getWeight()
                    : (variant != null && variant.getWeight() != null
                    ? variant.getWeight().doubleValue()
                    : (product != null && product.getProductWeight() != null
                    ? product.getProductWeight().doubleValue()
                    : 0.5));

            item.setLengthCm(length);
            item.setWidthCm(width);
            item.setHeightCm(height);
            item.setWeight(weight);
            double volumetric = (length * width * height) / 5000.0;
            item.setVolumetricWeight(volumetric);
            item.setChargeableWeight(Math.max(weight, volumetric));

            if (item.getSellerId() == null && product != null && product.getSellerId() != null) {
                item.setSellerId(product.getSellerId());
            }
        }



        @Override
        public String trackShipment(String awb) {

            try {

                String token = getToken();

                String url =
                        apiBaseUrl +
                                "/courier/track/awb/"
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
                                + "/courier/track/awb/"
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

                // Shiprocket order ids can exceed Integer range.
                ids.put(
                        Long.parseLong(
                                shiprocketOrderId.trim()
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
                                        + "/orders/cancel",
                                HttpMethod.POST,
                                entity,
                                String.class
                        );

                String responseBody =
                        response.getBody() != null
                                ? response.getBody()
                                : "";

                log.info(
                        "Shiprocket cancel response={}",
                        responseBody
                );

                if (response.getStatusCode()
                        .is2xxSuccessful()) {
                    return true;
                }

                String lower = responseBody.toLowerCase();
                // Treat already-cancelled / not-found as success for local cancel flow.
                return lower.contains("already cancel")
                        || lower.contains("already cancelled")
                        || lower.contains("canceled")
                        || lower.contains("not found")
                        || lower.contains("does not exist");

            } catch (HttpClientErrorException ex) {

                String apiBody =
                        ex.getResponseBodyAsString() != null
                                ? ex.getResponseBodyAsString()
                                : "";
                String lower = apiBody.toLowerCase();

                log.error(
                        "Shiprocket cancel API error={}",
                        apiBody,
                        ex
                );

                // Order already cancelled on Shiprocket — treat as success.
                if (lower.contains("already cancel")
                        || lower.contains("already cancelled")
                        || lower.contains("canceled")
                        || lower.contains("not found")
                        || lower.contains("does not exist")) {
                    return true;
                }

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



