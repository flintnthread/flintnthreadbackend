package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.OrderTrackingDTO;
import com.ecommerce.authdemo.dto.OrderTrackingResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
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
import java.util.Locale;

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

        private final SellerRepository sellerRepository;

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
            // If already linked but AWB missing (Ship Now done in Shiprocket UI), pull latest details.
            if (order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank()) {
                log.info(
                        "Shiprocket already linked for orderNumber={} shiprocketOrderId={} — syncing instead of create",
                        order.getOrderNumber(),
                        order.getShiprocketOrderId()
                );
                if (isBlank(order.getShiprocketAwbCode())
                        || isBlank(order.getShiprocketTrackingUrl())) {
                    return syncShipmentDetails(order);
                }
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

            String courierName = body.get("courier_name") != null
                    ? String.valueOf(body.get("courier_name"))
                    : null;

            if ((awb == null || awb.isBlank()) && shipmentId != null && !shipmentId.isBlank()) {
                Map<String, Object> assigned = tryAssignAwb(shipmentId);
                if (assigned != null) {
                    if (assigned.get("awb_code") != null) {
                        awb = String.valueOf(assigned.get("awb_code"));
                    }
                    if (assigned.get("courier_name") != null) {
                        courierName = String.valueOf(assigned.get("courier_name"));
                    }
                }
            }

            String trackingUrl = awb != null
                    ? "https://shiprocket.co/tracking/" + awb
                    : null;

            String shiprocketStatus = awb != null ? "awb_assigned" : "new";

            order.setShiprocketAwbCode(awb);
            order.setShiprocketCourierName(
                    courierName != null && !courierName.isBlank() ? courierName : "Shiprocket");
            order.setShiprocketTrackingUrl(trackingUrl);
            order.setShiprocketStatus(shiprocketStatus);

            // Persist Shiprocket IDs first so a later AWB update cannot leave the order unlinked.
            orderRepository.save(order);
            orderRepository.updateShipment(
                    order.getOrderNumber(),
                    awb,
                    order.getShiprocketCourierName(),
                    trackingUrl,
                    shiprocketStatus
            );

            return ShiprocketShipmentResult
                    .builder()
                    .shipmentId(shipmentId)
                    .awbCode(awb)
                    .trackingUrl(trackingUrl)
                    .courierName(order.getShiprocketCourierName())
                    .build();
        }

        /** Assign AWB via Shiprocket when create/adhoc did not return one immediately. */
        private Map<String, Object> tryAssignAwb(String shipmentId) {
            try {
                String token = getToken();
                String url = apiBaseUrl + "/courier/assign/awb";
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> payload = new HashMap<>();
                payload.put("shipment_id", List.of(Integer.parseInt(shipmentId.trim())));
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    return null;
                }
                Object responseData = body.get("response");
                if (responseData instanceof Map<?, ?> dataMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>) dataMap;
                    Object awbAssign = casted.get("data");
                    if (awbAssign instanceof Map<?, ?> assignMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> assign = (Map<String, Object>) assignMap;
                        return assign;
                    }
                }
                log.info("Shiprocket assign AWB response for shipment {}: {}", shipmentId, body);
                return body;
            } catch (Exception e) {
                log.warn("Shiprocket AWB assign failed for shipment {}: {}", shipmentId, e.getMessage());
                return null;
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

            // Customer name for Shiprocket: use the real name only — never append "Customer".
            String[] nameParts = splitCustomerName(order.getShippingName());
            payload.put("billing_customer_name", nameParts[0]);
            payload.put("billing_last_name", nameParts[1]);
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
         * (Settings → Pickup Addresses).
         * Prefer seller business / branch name from DB; only fall back to ASVI HOME FOODS
         * (or Admin Platform Settings default) when the seller has no usable pickup name.
         * If Shiprocket rejects the seller nickname, {@link #postCreateAdhocWithPickupFallback}
         * retries with the platform default.
         */
        private String defaultPickupLocation() {
            String configured = integrationSettings.getShiprocketPickupLocation();
            return configured != null && !configured.isBlank()
                    ? configured.trim()
                    : "ASVI HOME FOODS";
        }

        private String resolvePickupLocation(Long sellerId) {
            String fallback = defaultPickupLocation();

            // Optional explicit map: sellerId:PickupNickname
            String fromMap = resolvePickupFromSellerMap(sellerId);
            if (fromMap != null) {
                return fromMap;
            }

            String fromSeller = resolvePickupFromSellerProfile(sellerId);
            if (fromSeller != null) {
                log.info(
                        "Shiprocket pickup from seller profile sellerId={} pickup={}",
                        sellerId,
                        fromSeller
                );
                return fromSeller;
            }

            log.info(
                    "Shiprocket pickup falling back to platform default for sellerId={} pickup={}",
                    sellerId,
                    fallback
            );
            return fallback;
        }

        private String resolvePickupFromSellerMap(Long sellerId) {
            if (sellerId == null || pickupLocationBySeller == null || pickupLocationBySeller.isBlank()) {
                return null;
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
            return null;
        }

        /**
         * Use seller's registered pickup nickname: business name first, then branch name.
         * Returns null when missing so caller can fall back to ASVI HOME FOODS.
         */
        private String resolvePickupFromSellerProfile(Long sellerId) {
            if (sellerId == null) {
                return null;
            }
            return sellerRepository.findById(sellerId)
                    .map(seller -> {
                        String business = trimToNull(seller.getBusinessName());
                        if (business != null) {
                            return business;
                        }
                        String branch = trimToNull(seller.getBranchName());
                        if (branch != null) {
                            return branch;
                        }
                        // Warehouse street text is NOT a Shiprocket pickup nickname — skip it.
                        return null;
                    })
                    .orElse(null);
        }

        private static String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        /**
         * @return [firstName, lastName] — lastName may be empty; never forced to "Customer".
         */
        private static String[] splitCustomerName(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                return new String[]{"Customer", ""};
            }
            String trimmed = fullName.trim().replaceAll("\\s+", " ");
            // Strip a trailing "customer" label if it was previously appended.
            if (trimmed.toLowerCase(Locale.ROOT).endsWith(" customer")) {
                trimmed = trimmed.substring(0, trimmed.length() - " customer".length()).trim();
            }
            int space = trimmed.indexOf(' ');
            if (space > 0) {
                return new String[]{
                        trimmed.substring(0, space),
                        trimmed.substring(space + 1)
                };
            }
            return new String[]{trimmed, ""};
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
        public ShiprocketShipmentResult syncShipmentDetails(Order order) {
            if (order == null || order.getId() == null) {
                throw new IllegalArgumentException("Order is required for Shiprocket sync.");
            }

            try {
                Map<String, Object> remote = fetchShiprocketOrderPayload(order);
                if (remote == null || remote.isEmpty()) {
                    log.warn(
                            "Shiprocket sync found no remote order for orderNumber={}",
                            order.getOrderNumber()
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

                applyRemoteShiprocketFields(order, remote);
                order.setShiprocketSyncedAt(java.time.LocalDateTime.now());
                orderRepository.save(order);

                if (!isBlank(order.getShiprocketAwbCode())
                        || !isBlank(order.getShiprocketTrackingUrl())) {
                    orderRepository.updateShipment(
                            order.getOrderNumber(),
                            order.getShiprocketAwbCode(),
                            order.getShiprocketCourierName(),
                            order.getShiprocketTrackingUrl(),
                            order.getShiprocketStatus() != null
                                    ? order.getShiprocketStatus()
                                    : "awb_assigned"
                    );
                }

                log.info(
                        "Shiprocket sync saved orderNumber={} awb={} trackingUrl={} status={}",
                        order.getOrderNumber(),
                        order.getShiprocketAwbCode(),
                        order.getShiprocketTrackingUrl(),
                        order.getOrderStatus()
                );

                return ShiprocketShipmentResult.builder()
                        .shipmentId(order.getShiprocketShipmentId())
                        .awbCode(order.getShiprocketAwbCode())
                        .trackingUrl(order.getShiprocketTrackingUrl())
                        .courierName(order.getShiprocketCourierName() != null
                                ? order.getShiprocketCourierName()
                                : "Shiprocket")
                        .build();
            } catch (Exception e) {
                log.error(
                        "Shiprocket sync failed orderNumber={}: {}",
                        order.getOrderNumber(),
                        e.getMessage(),
                        e
                );
                throw new RuntimeException(
                        "Shiprocket sync failed: " + e.getMessage(),
                        e
                );
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> fetchShiprocketOrderPayload(Order order) {
            String token = getToken();
            Map<String, Object> merged = new HashMap<>();

            // 1) Track-by-shipment — most reliable AWB source after dashboard "Ship Now".
            if (!isBlank(order.getShiprocketShipmentId())
                    && order.getShiprocketShipmentId().trim().matches("^\\d+$")) {
                Map<String, Object> trackBody = getShiprocketJson(
                        token,
                        "/courier/track/shipment/" + order.getShiprocketShipmentId().trim()
                );
                mergeShiprocketTrackPayload(merged, trackBody);

                Map<String, Object> shipmentBody = unwrapDataMap(getShiprocketJson(
                        token,
                        "/shipments/" + order.getShiprocketShipmentId().trim()
                ));
                if (shipmentBody != null) {
                    merged.putAll(shipmentBody);
                }
            }

            // 2) Full order show by Shiprocket order id.
            if (!isBlank(order.getShiprocketOrderId())
                    && order.getShiprocketOrderId().trim().matches("^\\d+$")) {
                Map<String, Object> orderBody = unwrapDataMap(getShiprocketJson(
                        token,
                        "/orders/show/" + order.getShiprocketOrderId().trim()
                ));
                if (orderBody != null && !orderBody.isEmpty()) {
                    // Order payload wins for ids/status; keep any AWB already found from track.
                    String existingAwb = findFirstDeep(merged, "awb", "awb_code", "awbCode");
                    String existingCourier = findFirstDeep(
                            merged,
                            "courier_name",
                            "courier",
                            "sr_courier_name"
                    );
                    String existingTrackUrl = findFirstDeep(
                            merged,
                            "tracking_url",
                            "track_url",
                            "trackingUrl"
                    );
                    merged.putAll(orderBody);
                    if (!isBlank(existingAwb) && isBlank(findFirstDeep(merged, "awb", "awb_code", "awbCode"))) {
                        merged.put("awb", existingAwb);
                    }
                    if (!isBlank(existingCourier)
                            && isBlank(findFirstDeep(merged, "courier_name", "courier", "sr_courier_name"))) {
                        merged.put("courier_name", existingCourier);
                    }
                    if (!isBlank(existingTrackUrl)
                            && isBlank(findFirstDeep(merged, "tracking_url", "track_url", "trackingUrl"))) {
                        merged.put("tracking_url", existingTrackUrl);
                    }
                }
            }

            // 3) Search by channel order number (FNT…).
            if (isBlank(findFirstDeep(merged, "awb", "awb_code", "awbCode"))
                    && !isBlank(order.getOrderNumber())) {
                Map<String, Object> search = getShiprocketJson(
                        token,
                        "/orders?search=" + java.net.URLEncoder.encode(
                                order.getOrderNumber().trim(),
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );
                Map<String, Object> matched =
                        pickMatchingOrderFromSearch(search, order.getOrderNumber().trim());
                if (matched != null && !matched.isEmpty()) {
                    merged.putAll(matched);
                }
            }

            if (merged.isEmpty()) {
                return null;
            }
            log.info(
                    "Shiprocket sync payload ready orderNumber={} hasAwb={} keys={}",
                    order.getOrderNumber(),
                    !isBlank(findFirstDeep(merged, "awb", "awb_code", "awbCode")),
                    merged.keySet()
            );
            return merged;
        }

        @SuppressWarnings("unchecked")
        private void mergeShiprocketTrackPayload(
                Map<String, Object> target,
                Map<String, Object> trackBody
        ) {
            if (trackBody == null || trackBody.isEmpty()) {
                return;
            }
            Object trackingData = trackBody.get("tracking_data");
            if (!(trackingData instanceof Map<?, ?>)) {
                // Some responses nest under data.tracking_data
                Object data = trackBody.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    trackingData = dataMap.get("tracking_data");
                    if (trackingData == null) {
                        target.putAll((Map<String, Object>) dataMap);
                    }
                }
            }
            if (!(trackingData instanceof Map<?, ?> tdMap)) {
                String awb = findFirstDeep(trackBody, "awb", "awb_code", "awbCode");
                if (!isBlank(awb)) {
                    target.put("awb", awb);
                }
                return;
            }
            Map<String, Object> td = (Map<String, Object>) tdMap;
            String trackUrl = firstNonBlank(td, "track_url", "tracking_url", "trackingUrl");
            if (!isBlank(trackUrl)) {
                target.put("tracking_url", trackUrl.trim());
            }
            String status = firstNonBlank(td, "shipment_status", "track_status", "status");
            if (!isBlank(status)) {
                target.put("status", status.trim());
            }
            Object shipmentTrack = td.get("shipment_track");
            if (shipmentTrack instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> firstMap) {
                Map<String, Object> first = (Map<String, Object>) firstMap;
                String awb = firstNonBlank(first, "awb_code", "awb", "awbCode");
                if (!isBlank(awb)) {
                    target.put("awb", awb.trim());
                }
                String courier = firstNonBlank(
                        first,
                        "courier_name",
                        "courier",
                        "sr_courier_name",
                        "courierName"
                );
                if (!isBlank(courier)) {
                    target.put("courier_name", courier.trim());
                }
                if (isBlank(status)) {
                    String st = firstNonBlank(first, "current_status", "status");
                    if (!isBlank(st)) {
                        target.put("status", st.trim());
                    }
                }
            } else {
                String awb = findFirstDeep(td, "awb", "awb_code", "awbCode");
                if (!isBlank(awb)) {
                    target.put("awb", awb);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getShiprocketJson(String token, String pathAndQuery) {
            String url = apiBaseUrl + pathAndQuery;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> request = new HttpEntity<>(headers);
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        request,
                        Map.class
                );
                return response.getBody() != null ? response.getBody() : Map.of();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.warn(
                        "Shiprocket GET {} failed status={} body={}",
                        pathAndQuery,
                        e.getStatusCode(),
                        e.getResponseBodyAsString()
                );
                return Map.of();
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> unwrapDataMap(Map<String, Object> body) {
            if (body == null || body.isEmpty()) {
                return null;
            }
            Object data = body.get("data");
            if (data instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            if (data == null && (body.containsKey("id") || body.containsKey("channel_order_id"))) {
                return body;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> pickMatchingOrderFromSearch(
                Map<String, Object> searchBody,
                String channelOrderId
        ) {
            if (searchBody == null || searchBody.isEmpty()) {
                return null;
            }
            Object data = searchBody.get("data");
            List<Map<String, Object>> rows = new ArrayList<>();
            if (data instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        rows.add((Map<String, Object>) map);
                    }
                }
            } else if (data instanceof Map<?, ?> nested) {
                Object inner = ((Map<?, ?>) nested).get("data");
                if (inner instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            rows.add((Map<String, Object>) map);
                        }
                    }
                } else {
                    rows.add((Map<String, Object>) nested);
                }
            }

            for (Map<String, Object> row : rows) {
                String channel = firstNonBlank(row, "channel_order_id", "channelOrderId");
                if (channelOrderId.equalsIgnoreCase(channel != null ? channel : "")) {
                    return row;
                }
            }
            if (rows.size() == 1) {
                return rows.get(0);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private void applyRemoteShiprocketFields(Order order, Map<String, Object> remote) {
            String srOrderId = firstNonBlank(remote, "id", "order_id", "orderId");
            if (!isBlank(srOrderId) && srOrderId.matches("^\\d+$")) {
                order.setShiprocketOrderId(srOrderId.trim());
            }

            String shipmentId = firstNonBlank(remote, "shipment_id", "shipmentId");
            String awb = firstNonBlank(remote, "awb_code", "awb", "awbCode");
            String courier = firstNonBlank(
                    remote,
                    "courier_name",
                    "courierName",
                    "courier",
                    "courier_company_name",
                    "sr_courier_name"
            );
            String status = firstNonBlank(
                    remote,
                    "status",
                    "current_status",
                    "shipment_status",
                    "status_code"
            );
            String trackingUrl = firstNonBlank(
                    remote,
                    "tracking_url",
                    "trackingUrl",
                    "track_url",
                    "trackUrl"
            );

            Object shipmentsObj = remote.get("shipments");
            if (shipmentsObj instanceof List<?> shipments && !shipments.isEmpty()) {
                for (Object item : shipments) {
                    if (!(item instanceof Map<?, ?> shipmentMap)) {
                        continue;
                    }
                    Map<String, Object> shipment = (Map<String, Object>) shipmentMap;
                    if (isBlank(shipmentId)) {
                        shipmentId = firstNonBlank(shipment, "id", "shipment_id", "shipmentId");
                    }
                    if (isBlank(awb)) {
                        awb = firstNonBlank(shipment, "awb", "awb_code", "awbCode");
                    }
                    if (isBlank(courier)) {
                        courier = firstNonBlank(
                                shipment,
                                "courier",
                                "courier_name",
                                "courierName",
                                "sr_courier_name"
                        );
                    }
                    if (isBlank(status)) {
                        status = firstNonBlank(
                                shipment,
                                "status",
                                "current_status",
                                "shipment_status"
                        );
                    }
                    if (isBlank(trackingUrl)) {
                        trackingUrl = firstNonBlank(
                                shipment,
                                "tracking_url",
                                "track_url",
                                "trackingUrl"
                        );
                    }
                    if (!isBlank(awb)) {
                        break;
                    }
                }
            }

            Object awbDataObj = remote.get("awb_data");
            if (awbDataObj instanceof Map<?, ?> awbDataMap) {
                Map<String, Object> awbData = (Map<String, Object>) awbDataMap;
                if (isBlank(awb)) {
                    awb = firstNonBlank(awbData, "awb", "awb_code", "awbCode");
                }
                if (isBlank(courier)) {
                    courier = firstNonBlank(awbData, "courier_name", "courier", "courierName");
                }
            }

            // Shiprocket nests AWB under varying keys — deep scan as last resort.
            if (isBlank(awb)) {
                awb = findFirstDeep(remote, "awb_code", "awb", "awbCode");
            }
            if (isBlank(courier)) {
                courier = findFirstDeep(
                        remote,
                        "courier_name",
                        "sr_courier_name",
                        "courierName",
                        "courier"
                );
            }
            if (isBlank(trackingUrl)) {
                trackingUrl = findFirstDeep(remote, "tracking_url", "track_url", "trackingUrl");
            }
            if (isBlank(shipmentId)) {
                shipmentId = findFirstDeep(remote, "shipment_id", "shipmentId");
            }

            if (!isBlank(shipmentId)) {
                order.setShiprocketShipmentId(shipmentId.trim());
            }
            if (!isBlank(awb)) {
                // AWB can arrive as a number from JSON.
                order.setShiprocketAwbCode(awb.trim().replaceAll("\\.0$", ""));
            }
            if (!isBlank(courier)) {
                order.setShiprocketCourierName(courier.trim());
            } else if (isBlank(order.getShiprocketCourierName()) && !isBlank(awb)) {
                order.setShiprocketCourierName("Shiprocket");
            }

            String resolvedAwb = !isBlank(order.getShiprocketAwbCode())
                    ? order.getShiprocketAwbCode().trim()
                    : null;
            String resolvedTracking = !isBlank(trackingUrl) ? trackingUrl.trim() : null;
            if (isBlank(resolvedTracking) && !isBlank(resolvedAwb)) {
                resolvedTracking = "https://shiprocket.co/tracking/" + resolvedAwb;
            }
            if (!isBlank(resolvedTracking)) {
                order.setShiprocketTrackingUrl(resolvedTracking);
            }

            String mappedStatus = mapWebhookStatusToOrderStatus(status);
            if (!isBlank(mappedStatus)) {
                order.setShiprocketStatus(mappedStatus);
                if (isEarlyFulfillmentStatus(order.getOrderStatus())
                        || "awb_assigned".equalsIgnoreCase(mappedStatus)
                        || "pickup_scheduled".equalsIgnoreCase(mappedStatus)
                        || "picked_up".equalsIgnoreCase(mappedStatus)
                        || "in_transit".equalsIgnoreCase(mappedStatus)
                        || "out_for_delivery".equalsIgnoreCase(mappedStatus)
                        || "delivered".equalsIgnoreCase(mappedStatus)) {
                    order.setOrderStatus(mappedStatus);
                }
            } else if (!isBlank(resolvedAwb) && isEarlyFulfillmentStatus(order.getOrderStatus())) {
                order.setShiprocketStatus("awb_assigned");
                order.setOrderStatus("awb_assigned");
            }

            if (isBlank(resolvedAwb)) {
                log.warn(
                        "Shiprocket sync parsed order payload but AWB still empty. keys={} shipmentsType={}",
                        remote.keySet(),
                        remote.get("shipments") == null
                                ? "null"
                                : remote.get("shipments").getClass().getSimpleName()
                );
            }
        }

        @SuppressWarnings("unchecked")
        private String findFirstDeep(Object node, String... keys) {
            if (node == null || keys == null || keys.length == 0) {
                return null;
            }
            if (node instanceof Map<?, ?> map) {
                Map<String, Object> asMap = (Map<String, Object>) map;
                String direct = firstNonBlank(asMap, keys);
                if (!isBlank(direct)) {
                    return direct;
                }
                for (Object value : asMap.values()) {
                    String nested = findFirstDeep(value, keys);
                    if (!isBlank(nested)) {
                        return nested;
                    }
                }
            } else if (node instanceof List<?> list) {
                for (Object item : list) {
                    String nested = findFirstDeep(item, keys);
                    if (!isBlank(nested)) {
                        return nested;
                    }
                }
            }
            return null;
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
                if (webhookData == null || webhookData.isEmpty()) {
                    log.warn("Shiprocket webhook ignored: empty payload");
                    return;
                }

                Map<String, Object> event = unwrapWebhookEvent(webhookData);

                String awb = firstNonBlank(
                        event,
                        "awb",
                        "awb_code",
                        "awbCode"
                );
                String channelOrderId = firstNonBlank(
                        event,
                        "channel_order_id",
                        "channelOrderId",
                        "order_number",
                        "orderNumber"
                );
                String shiprocketOrderId = firstNonBlank(
                        event,
                        "sr_order_id",
                        "srOrderId",
                        "shiprocket_order_id",
                        "shiprocketOrderId",
                        "order_id",
                        "orderId"
                );
                String shipmentId = firstNonBlank(
                        event,
                        "shipment_id",
                        "shipmentId"
                );
                String courierName = firstNonBlank(
                        event,
                        "courier_name",
                        "courierName",
                        "courier"
                );
                String currentStatus = firstNonBlank(
                        event,
                        "current_status",
                        "currentStatus",
                        "shipment_status",
                        "shipmentStatus",
                        "status"
                );
                String trackingUrl = firstNonBlank(
                        event,
                        "tracking_url",
                        "trackingUrl",
                        "track_url",
                        "trackUrl"
                );

                if (isBlank(awb)
                        && isBlank(channelOrderId)
                        && isBlank(shiprocketOrderId)
                        && isBlank(currentStatus)) {
                    log.warn(
                            "Shiprocket webhook ignored: no awb/order identifiers. keys={}",
                            event.keySet()
                    );
                    return;
                }

                Order order = resolveOrderFromWebhook(
                        channelOrderId,
                        shiprocketOrderId,
                        awb
                );

                if (order == null) {
                    log.warn(
                            "Shiprocket webhook order not found channelOrderId={} srOrderId={} awb={}",
                            channelOrderId,
                            shiprocketOrderId,
                            awb
                    );
                    return;
                }

                if (!isBlank(awb)) {
                    order.setShiprocketAwbCode(awb.trim());
                }
                if (!isBlank(shiprocketOrderId)
                        && !shiprocketOrderId.matches("(?i)^FNT\\d+")
                        && !shiprocketOrderId.equalsIgnoreCase(
                        String.valueOf(order.getId()))) {
                    // Prefer Shiprocket's own id — skip when value is our channel order number.
                    if (isBlank(order.getShiprocketOrderId())
                            || !order.getShiprocketOrderId().equals(shiprocketOrderId.trim())) {
                        // Only overwrite when channel id was not mistaken for sr id.
                        if (isBlank(channelOrderId)
                                || !shiprocketOrderId.trim().equalsIgnoreCase(channelOrderId.trim())) {
                            order.setShiprocketOrderId(shiprocketOrderId.trim());
                        }
                    }
                }
                if (!isBlank(shipmentId)) {
                    order.setShiprocketShipmentId(shipmentId.trim());
                }
                if (!isBlank(courierName)) {
                    order.setShiprocketCourierName(courierName.trim());
                } else if (isBlank(order.getShiprocketCourierName())) {
                    order.setShiprocketCourierName("Shiprocket");
                }

                String resolvedAwb = !isBlank(order.getShiprocketAwbCode())
                        ? order.getShiprocketAwbCode().trim()
                        : null;
                String resolvedTracking = !isBlank(trackingUrl)
                        ? trackingUrl.trim()
                        : null;
                if (isBlank(resolvedTracking) && !isBlank(resolvedAwb)) {
                    resolvedTracking = "https://shiprocket.co/tracking/" + resolvedAwb;
                }
                if (!isBlank(resolvedTracking)) {
                    order.setShiprocketTrackingUrl(resolvedTracking);
                }

                String mappedStatus = mapWebhookStatusToOrderStatus(currentStatus);
                if (!isBlank(mappedStatus)) {
                    order.setShiprocketStatus(mappedStatus);
                    order.setOrderStatus(mappedStatus);
                } else if (!isBlank(resolvedAwb)
                        && isBlank(order.getShiprocketStatus())) {
                    order.setShiprocketStatus("awb_assigned");
                    order.setOrderStatus("awb_assigned");
                } else if (!isBlank(resolvedAwb)
                        && !"awb_assigned".equalsIgnoreCase(order.getOrderStatus())
                        && isEarlyFulfillmentStatus(order.getOrderStatus())) {
                    order.setShiprocketStatus("awb_assigned");
                    order.setOrderStatus("awb_assigned");
                }

                order.setShiprocketSyncedAt(java.time.LocalDateTime.now());
                orderRepository.save(order);

                log.info(
                        "Shiprocket webhook saved orderNumber={} awb={} trackingUrl={} status={}",
                        order.getOrderNumber(),
                        order.getShiprocketAwbCode(),
                        order.getShiprocketTrackingUrl(),
                        order.getOrderStatus()
                );

            } catch (Exception e) {

                log.error(
                        "Webhook processing failed",
                        e
                );
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> unwrapWebhookEvent(Map<String, Object> payload) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> nested) {
                Map<String, Object> merged = new HashMap<>(payload);
                merged.putAll((Map<String, Object>) nested);
                return merged;
            }
            return payload;
        }

        private Order resolveOrderFromWebhook(
                String channelOrderId,
                String shiprocketOrderId,
                String awb
        ) {
            if (!isBlank(channelOrderId)) {
                Optional<Order> byNumber =
                        orderRepository.findByOrderNumber(channelOrderId.trim());
                if (byNumber.isPresent()) {
                    return byNumber.get();
                }
            }

            if (!isBlank(awb)) {
                Optional<Order> byAwb =
                        orderRepository.findByShiprocketAwbCode(awb.trim());
                if (byAwb.isPresent()) {
                    return byAwb.get();
                }
            }

            if (!isBlank(shiprocketOrderId)) {
                String srId = shiprocketOrderId.trim();
                Optional<Order> bySr =
                        orderRepository.findByShiprocketOrderId(srId);
                if (bySr.isPresent()) {
                    return bySr.get();
                }
                // Some payloads put our FNT order number in order_id.
                Optional<Order> byNumber =
                        orderRepository.findByOrderNumber(srId);
                if (byNumber.isPresent()) {
                    return byNumber.get();
                }
                if (srId.matches("^\\d+$")) {
                    try {
                        long id = Long.parseLong(srId);
                        Optional<Order> byId = orderRepository.findById(id);
                        if (byId.isPresent()) {
                            return byId.get();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return null;
        }

        private String mapWebhookStatusToOrderStatus(String sourceStatus) {
            if (isBlank(sourceStatus)) {
                return null;
            }
            String normalized = sourceStatus.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("-", "_")
                    .replace(" ", "_");

            return switch (normalized) {
                case "new" -> "new";
                case "confirmed" -> "confirmed";
                case "processing" -> "processing";
                case "packed" -> "packed";
                case "awb_assigned", "awbassigned" -> "awb_assigned";
                case "pickup_scheduled", "pickup_generated", "pickup_queued" -> "pickup_scheduled";
                case "picked_up", "shipped" -> "picked_up";
                case "in_transit", "intransit" -> "in_transit";
                case "out_for_delivery", "ofd" -> "out_for_delivery";
                case "delivered" -> "delivered";
                case "cancelled", "canceled" -> "cancelled";
                case "rto_initiated", "rto_in_transit" -> "rto_initiated";
                case "rto_delivered" -> "rto_delivered";
                case "return_initiated", "returned" -> "returned";
                default -> null;
            };
        }

        private boolean isEarlyFulfillmentStatus(String status) {
            if (isBlank(status)) {
                return true;
            }
            String s = status.trim().toLowerCase(Locale.ROOT);
            return s.equals("new")
                    || s.equals("confirmed")
                    || s.equals("processing")
                    || s.equals("packed")
                    || s.equals("accepted");
        }

        private String firstNonBlank(Map<String, Object> source, String... keys) {
            if (source == null || source.isEmpty() || keys == null) {
                return null;
            }
            for (String key : keys) {
                Object value = source.get(key);
                if (value == null) {
                    continue;
                }
                String asText = String.valueOf(value).trim();
                if (!asText.isEmpty() && !"null".equalsIgnoreCase(asText)) {
                    return asText;
                }
            }
            return null;
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
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
                                order != null && order.getShiprocketTrackingUrl() != null
                                        && !order.getShiprocketTrackingUrl().isBlank()
                                        ? order.getShiprocketTrackingUrl()
                                        : "https://shiprocket.co/tracking/" + awb
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



