package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.Enum.OrderStatus;
import com.ecommerce.authdemo.dto.OrderTrackingDTO;
import com.ecommerce.authdemo.dto.OrderTrackingResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.OrderStatusHistoryRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.service.PlatformIntegrationSettings;
import com.ecommerce.authdemo.service.ShiprocketService;
import com.ecommerce.authdemo.service.ShiprocketSyncLogService;
import com.ecommerce.authdemo.util.ShiprocketOrderPricing;
import com.ecommerce.authdemo.util.ShiprocketPickupSupport;
import com.ecommerce.authdemo.util.ShiprocketPushOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.cloudinary.json.JSONArray;
import org.cloudinary.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        private final OrderStatusHistoryRepository orderStatusHistoryRepository;

        private final ProductRepository productRepository;

        private final ProductVariantRepository productVariantRepository;

        private final SellerRepository sellerRepository;

        private final PlatformIntegrationSettings integrationSettings;

        private final ShiprocketSyncLogService shiprocketSyncLogService;

        private final ObjectMapper objectMapper;

        @Value("${shiprocket.api.base-url}")
        private String apiBaseUrl;

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
        @Transactional
        public ShiprocketShipmentResult createShipment(
                Order order
        ) {

            if (order == null || order.getId() == null) {
                throw new IllegalArgumentException("Order is required for Shiprocket shipment.");
            }

            boolean hasSrOrder = order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank();
            boolean hasAwb = !isBlank(order.getShiprocketAwbCode());
            boolean pushFailed = isFailedPushStatus(order.getShiprocketStatus());

            // Already courier-assigned: sync only.
            if (hasSrOrder && hasAwb) {
                log.info(
                        "Shiprocket already linked for orderNumber={} shiprocketOrderId={} — syncing instead of create",
                        order.getOrderNumber(),
                        order.getShiprocketOrderId()
                );
                if (isBlank(order.getShiprocketTrackingUrl())) {
                    return syncShipmentDetails(order);
                }
                return ShiprocketShipmentResult.builder()
                        .shipmentId(order.getShiprocketShipmentId())
                        .awbCode(order.getShiprocketAwbCode())
                        .trackingUrl(order.getShiprocketTrackingUrl())
                        .courierName(order.getShiprocketCourierName() != null
                                ? order.getShiprocketCourierName()
                                : "Shiprocket")
                        .alreadyExists(true)
                        .message("Shipment already exists on Shiprocket")
                        .build();
            }

            // Successful create awaiting manual courier assign — never create a duplicate.
            if (hasSrOrder && !pushFailed) {
                log.info(
                        "Shiprocket shipment already created for orderNumber={} (awaiting courier) — skipping recreate",
                        order.getOrderNumber()
                );
                return ShiprocketShipmentResult.builder()
                        .shipmentId(order.getShiprocketShipmentId())
                        .awbCode(order.getShiprocketAwbCode())
                        .trackingUrl(order.getShiprocketTrackingUrl())
                        .courierName(order.getShiprocketCourierName() != null
                                ? order.getShiprocketCourierName()
                                : "Shiprocket")
                        .alreadyExists(true)
                        .message("Shipment already created on Shiprocket. Assign courier in Shiprocket, then sync.")
                        .build();
            }

            // Prior push failed — cancel leftover SR order if any, then recreate.
            if (hasSrOrder && pushFailed) {
                log.warn(
                        "Retrying failed Shiprocket push orderNumber={} oldSrOrderId={} status={}",
                        order.getOrderNumber(),
                        order.getShiprocketOrderId(),
                        order.getShiprocketStatus()
                );
                try {
                    cancelShipment(order.getShiprocketOrderId());
                } catch (Exception cancelEx) {
                    log.warn("Shiprocket cancel before recreate failed: {}", cancelEx.getMessage());
                }
                order.setShiprocketOrderId(null);
                order.setShiprocketShipmentId(null);
                order.setShiprocketAwbCode(null);
                order.setShiprocketCourierName(null);
                order.setShiprocketTrackingUrl(null);
                order.setShiprocketStatus(null);
                order.setShiprocketPushedAt(null);
                order.setShiprocketSyncedAt(null);
                orderRepository.save(order);
            }

            List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
            if (allItems == null || allItems.isEmpty()) {
                throw new RuntimeException(
                        "No order items found for Shiprocket order " + order.getOrderNumber()
                );
            }

            for (OrderItem item : allItems) {
                enrichOrderItemFromCatalog(item);
            }

            Map<Long, List<OrderItem>> itemsBySeller = groupItemsBySeller(allItems);
            if (itemsBySeller.isEmpty()) {
                throw new RuntimeException(
                        "No seller found on order items for Shiprocket order " + order.getOrderNumber()
                );
            }

            boolean multiSeller = itemsBySeller.size() > 1;
            ShiprocketShipmentResult firstSuccess = null;
            RuntimeException lastError = null;
            int successCount = 0;

            for (Map.Entry<Long, List<OrderItem>> entry : itemsBySeller.entrySet()) {
                Long sellerId = entry.getKey();
                ShiprocketPushOptions options = multiSeller
                        ? ShiprocketPushOptions.forSeller(sellerId)
                        : ShiprocketPushOptions.empty();
                Map<String, Object> payload = null;
                try {
                    payload = buildShipmentPayload(order, entry.getValue(), options);
                    Map<String, Object> apiPayload = sanitizePayloadForApi(payload);
                    validateCodAmount(order, apiPayload);
                    Map<String, Object> body = postCreateAdhocWithPickupFallback(order, payload);
                    logOrderPush(
                            order,
                            body.get("order_id") != null ? String.valueOf(body.get("order_id")) : null,
                            "success",
                            apiPayload,
                            body,
                            null
                    );
                    if (firstSuccess == null) {
                        firstSuccess = persistShiprocketCreateResponse(order, body);
                    } else {
                        log.info(
                                "Shiprocket multi-seller push OK orderNumber={} sellerId={} srOrderId={} (primary already saved)",
                                order.getOrderNumber(),
                                sellerId,
                                body.get("order_id")
                        );
                    }
                    successCount++;
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    String apiBody = e.getResponseBodyAsString();
                    log.error(
                            "Shiprocket API error orderNumber={} sellerId={} body={}",
                            order.getOrderNumber(),
                            sellerId,
                            apiBody,
                            e
                    );
                    logOrderPush(
                            order,
                            null,
                            "failed",
                            payload != null ? sanitizePayloadForApi(payload) : Map.of(),
                            Map.of("httpStatus", e.getStatusCode().value(), "body", apiBody != null ? apiBody : ""),
                            apiBody
                    );
                    lastError = new RuntimeException(
                            "Shiprocket API error: " + (apiBody != null && !apiBody.isBlank() ? apiBody : e.getMessage()),
                            e
                    );
                } catch (Exception e) {
                    logOrderPush(
                            order,
                            null,
                            "failed",
                            payload != null ? sanitizePayloadForApi(payload) : Map.of(),
                            null,
                            e.getMessage()
                    );
                    lastError = e instanceof RuntimeException re
                            ? re
                            : new RuntimeException("Shipment creation failed: " + e.getMessage(), e);
                }
            }

            if (firstSuccess == null) {
                throw lastError != null
                        ? lastError
                        : new RuntimeException("Shipment creation failed for all sellers");
            }
            if (multiSeller) {
                log.info(
                        "Shiprocket multi-seller auto-push orderNumber={} sellers={} succeeded={}",
                        order.getOrderNumber(),
                        itemsBySeller.size(),
                        successCount
                );
            }
            return firstSuccess;
        }

        private static boolean isFailedPushStatus(String shiprocketStatus) {
            if (shiprocketStatus == null || shiprocketStatus.isBlank()) {
                return false;
            }
            String s = shiprocketStatus.trim().toLowerCase(Locale.ENGLISH);
            return s.equals("pending") || s.startsWith("pending:");
        }

        /**
         * Create on Shiprocket. Never fall back to ASVI/platform default —
         * pickup must be the seller business name. If missing on Shiprocket, register it once then retry.
         */
        private Map<String, Object> postCreateAdhocWithPickupFallback(
                Order order,
                Map<String, Object> payload
        ) {
            try {
                return postCreateAdhoc(order, payload);
            } catch (RuntimeException first) {
                String detail = first.getMessage() != null ? first.getMessage() : "";
                if (first instanceof HttpClientErrorException httpEx) {
                    String apiBody = httpEx.getResponseBodyAsString();
                    if (apiBody != null && !apiBody.isBlank()) {
                        detail = detail + " " + apiBody;
                    }
                }
                String msg = detail.toLowerCase(Locale.ENGLISH);
                boolean pickupIssue = msg.contains("pickup")
                        || msg.contains("wrong pickup")
                        || msg.contains("warehouse");
                if (!pickupIssue) {
                    throw first;
                }

                Object usedPickup = payload.get("pickup_location");
                String used = usedPickup != null ? String.valueOf(usedPickup) : "";
                log.warn(
                        "Shiprocket rejected pickup={} for orderNumber={} — ensuring seller pickup exists and retrying once",
                        used,
                        order.getOrderNumber()
                );

                Long sellerId = null;
                Object pickupSeller = payload.get("_pickup_seller_id");
                if (pickupSeller instanceof Number number) {
                    sellerId = number.longValue();
                } else if (pickupSeller != null) {
                    try {
                        sellerId = Long.parseLong(String.valueOf(pickupSeller));
                    } catch (NumberFormatException ignored) {
                        // fall through
                    }
                }
                if (sellerId == null) {
                    sellerId = findPrimarySellerId(order);
                }
                if (sellerId != null) {
                    sellerRepository.findById(sellerId).ifPresent(seller ->
                            ensureSellerPickupRegistered(seller, used));
                }
                payload.put("pickup_location", used);
                return postCreateAdhoc(order, payload);
            }
        }

        private Long findPrimarySellerId(Order order) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            if (items == null) {
                return null;
            }
            for (OrderItem item : items) {
                if (item.getSellerId() != null) {
                    return item.getSellerId();
                }
                if (item.getProductId() != null) {
                    Long sid = productRepository.findById(item.getProductId())
                            .map(Product::getSellerId)
                            .orElse(null);
                    if (sid != null) {
                        return sid;
                    }
                }
            }
            return null;
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

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(sanitizePayloadForApi(payload), headers);
            log.info(
                    "Shiprocket Payload orderNumber={} pickup={} phone={} sub_total={} grand_total={} items={}",
                    order.getOrderNumber(),
                    payload.get("pickup_location"),
                    payload.get("billing_phone"),
                    payload.get("sub_total"),
                    payload.get("grand_total"),
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
                    && !"null".equalsIgnoreCase(String.valueOf(body.get("courier_name")))
                    && !String.valueOf(body.get("courier_name")).isBlank()
                    ? String.valueOf(body.get("courier_name"))
                    : null;

            // Do not call /courier/assign/awb — courier is assigned manually in Shiprocket.

            String trackingUrl = awb != null
                    ? "https://shiprocket.co/tracking/" + awb
                    : null;

            // Shipment exists on Shiprocket; courier/AWB assigned later in Shiprocket dashboard.
            String shiprocketStatus = awb != null ? "awb_assigned" : "awaiting_courier";

            order.setShiprocketAwbCode(awb);
            order.setShiprocketCourierName(courierName);
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

            if (awb == null || awb.isBlank()) {
                log.info(
                        "Shiprocket shipment created without AWB orderNumber={} shipmentId={} — assign courier in Shiprocket, then Sync",
                        order.getOrderNumber(),
                        shipmentId
                );
            }

            return ShiprocketShipmentResult
                    .builder()
                    .shipmentId(shipmentId)
                    .awbCode(awb)
                    .trackingUrl(trackingUrl)
                    .courierName(order.getShiprocketCourierName())
                    .message(awb != null
                            ? "Shipment created on Shiprocket"
                            : "Shipment created on Shiprocket. Assign courier in Shiprocket, then Sync Now.")
                    .build();
        }

        private Map<String, Object> buildShipmentPayload(
                Order order,
                List<OrderItem> items,
                ShiprocketPushOptions pushOptions
        ) {
            if (items == null || items.isEmpty()) {
                throw new RuntimeException(
                        "No order items found for Shiprocket order " + order.getOrderNumber()
                );
            }

            boolean scopedPush = pushOptions != null && pushOptions.isScoped();
            List<ShiprocketOrderPricing.LineInput> pricingLines = new ArrayList<>();

            double totalWeight = 0;
            double maxLength = 1;
            double maxWidth = 1;
            double maxHeight = 1;
            Long primarySellerId = null;
            BigDecimal scopedItemTotal = BigDecimal.ZERO;

            for (OrderItem item : items) {
                enrichOrderItemFromCatalog(item);

                Long productSellerId = null;
                if (item.getProductId() != null) {
                    productSellerId = productRepository.findById(item.getProductId())
                            .map(Product::getSellerId)
                            .orElse(null);
                }
                Long itemSellerId = ShiprocketPickupSupport.resolveSellerId(
                        productSellerId,
                        item.getSellerId()
                );
                if (primarySellerId == null) {
                    primarySellerId = itemSellerId;
                }

                int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                BigDecimal unitPrice = ShiprocketOrderPricing.toMoney(item.getPrice());
                BigDecimal lineTotal = ShiprocketOrderPricing.toMoney(item.getTotal());
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0
                        && lineTotal.compareTo(BigDecimal.ZERO) <= 0
                        && item.getVariantId() != null) {
                    ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                    if (variant != null) {
                        BigDecimal catalogPrice = firstPositivePrice(
                                variant.getFinalPrice(),
                                variant.getSellingPrice(),
                                variant.getBasePrice()
                        );
                        if (catalogPrice != null && catalogPrice.compareTo(BigDecimal.ZERO) > 0) {
                            unitPrice = catalogPrice;
                            lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                        }
                    }
                }
                scopedItemTotal = scopedItemTotal.add(
                        lineTotal.compareTo(BigDecimal.ZERO) > 0
                                ? lineTotal
                                : unitPrice.multiply(BigDecimal.valueOf(qty))
                );

                pricingLines.add(new ShiprocketOrderPricing.LineInput(
                        item.getProductName() != null && !item.getProductName().isBlank()
                                ? item.getProductName()
                                : "Product",
                        item.getSku() != null && !item.getSku().isBlank()
                                ? item.getSku()
                                : "SKU-" + item.getProductId(),
                        item.getHsnCode() != null && !item.getHsnCode().isBlank()
                                ? item.getHsnCode()
                                : "0000",
                        qty,
                        unitPrice,
                        lineTotal
                ));

                double lineWeight = item.getChargeableWeight() != null
                        ? item.getChargeableWeight()
                        : (item.getWeight() != null ? item.getWeight() : 0.5);
                totalWeight += lineWeight * qty;

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

            if (primarySellerId == null && pushOptions != null) {
                primarySellerId = pushOptions.sellerId();
            }

            BigDecimal orderAmountForPricing = scopedPush
                    ? scopedItemTotal
                    : ShiprocketOrderPricing.toMoney(order.getTotalAmount());
            if (orderAmountForPricing.compareTo(BigDecimal.ZERO) <= 0 && scopedItemTotal.compareTo(BigDecimal.ZERO) > 0) {
                orderAmountForPricing = scopedItemTotal;
            }

            ShiprocketOrderPricing.PricedPayload priced = ShiprocketOrderPricing.build(
                    pricingLines,
                    orderAmountForPricing,
                    scopedPush ? BigDecimal.ZERO : ShiprocketOrderPricing.toMoney(order.getShippingAmount()),
                    scopedPush ? BigDecimal.ZERO : ShiprocketOrderPricing.toMoney(order.getDiscountAmount()),
                    null
            );

            Map<String, Object> payload = new LinkedHashMap<>();
            String shiprocketOrderRef = order.getOrderNumber();
            if (scopedPush && primarySellerId != null) {
                shiprocketOrderRef = order.getOrderNumber() + "-S" + primarySellerId;
            }
            payload.put("order_id", shiprocketOrderRef);
            payload.put("order_date", LocalDate.now().toString());

            String pickupNickname = resolvePickupLocation(primarySellerId);
            payload.put("pickup_location", pickupNickname);
            payload.put("_pickup_seller_id", primarySellerId);

            String[] nameParts = splitCustomerName(order.getShippingName());
            payload.put("billing_customer_name", nameParts[0]);
            payload.put("billing_last_name", nameParts[1]);
            payload.put("billing_phone", normalizeIndianMobile(order.getShippingPhone()));
            payload.put("billing_email",
                    order.getShippingEmail() != null ? order.getShippingEmail() : "support@flintnthread.in");

            String billingAddress = order.getShippingAddress1();
            if (order.getShippingAddress2() != null && !order.getShippingAddress2().isBlank()) {
                billingAddress = (billingAddress != null ? billingAddress + ", " : "")
                        + order.getShippingAddress2();
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
            payload.put("order_items", priced.orderItems());
            payload.put(
                    "payment_method",
                    isCodPaymentMethod(order.getPaymentMethod()) ? "COD" : "Prepaid"
            );
            payload.put("sub_total", priced.subTotal().doubleValue());
            payload.put("shipping_charges", priced.shippingCharges().doubleValue());
            payload.put("total_discount", priced.totalDiscount().doubleValue());
            payload.put("grand_total", ShiprocketOrderPricing.computeGrandTotal(priced).doubleValue());
            log.info(
                    "Shiprocket pricing orderNumber={} scoped={} sellerId={} orderPlacingTotal={} shiprocketSubTotal={} grandTotal={} shippingCharges={} discount={}",
                    order.getOrderNumber(),
                    scopedPush,
                    primarySellerId,
                    priced.orderTotal(),
                    priced.subTotal(),
                    ShiprocketOrderPricing.computeGrandTotal(priced),
                    priced.shippingCharges(),
                    priced.totalDiscount()
            );

            payload.put("length", Math.max(maxLength, 1));
            payload.put("breadth", Math.max(maxWidth, 1));
            payload.put("height", Math.max(maxHeight, 1));
            payload.put("weight", totalWeight > 0 ? totalWeight : 0.5);
            payload.put(
                    "comment",
                    "FNT Order " + order.getOrderNumber()
                            + (scopedPush && primarySellerId != null ? " seller " + primarySellerId : "")
            );
            return payload;
        }

        private Map<Long, List<OrderItem>> groupItemsBySeller(List<OrderItem> items) {
            Map<Long, List<OrderItem>> bySeller = new LinkedHashMap<>();
            for (OrderItem item : items) {
                Long productSellerId = null;
                if (item.getProductId() != null) {
                    productSellerId = productRepository.findById(item.getProductId())
                            .map(Product::getSellerId)
                            .orElse(null);
                }
                Long sellerId = ShiprocketPickupSupport.resolveSellerId(productSellerId, item.getSellerId());
                if (sellerId == null) {
                    continue;
                }
                bySeller.computeIfAbsent(sellerId, ignored -> new ArrayList<>()).add(item);
            }
            return bySeller;
        }

        private static Map<String, Object> sanitizePayloadForApi(Map<String, Object> payload) {
            Map<String, Object> api = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                if (entry.getKey() != null && entry.getKey().startsWith("_")) {
                    continue;
                }
                api.put(entry.getKey(), entry.getValue());
            }
            return api;
        }

        private void validateCodAmount(Order order, Map<String, Object> apiPayload) {
            if (!isCodPaymentMethod(order.getPaymentMethod())) {
                return;
            }
            double subTotal = toDouble(apiPayload.get("sub_total"));
            double grandTotal = toDouble(apiPayload.get("grand_total"));
            if (subTotal <= 0 && grandTotal <= 0) {
                throw new RuntimeException(
                        "COD order amount is zero. Check order item prices/totals before pushing to Shiprocket."
                );
            }
            // Shiprocket recalculates from line items — ensure selling prices yield a positive grand total.
            if (grandTotal <= 0 && subTotal > 0) {
                throw new RuntimeException(
                        "COD grand total is zero after line-item pricing. Check selling_price on order items."
                );
            }
        }

        private static double toDouble(Object value) {
            if (value == null) {
                return 0;
            }
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        private static BigDecimal firstPositivePrice(BigDecimal... values) {
            if (values == null) {
                return null;
            }
            for (BigDecimal value : values) {
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
            }
            return null;
        }

        private void logOrderPush(
                Order order,
                String shiprocketOrderId,
                String status,
                Map<String, Object> requestPayload,
                Map<String, Object> responsePayload,
                String errorMessage
        ) {
            if (order == null || order.getId() == null) {
                return;
            }
            try {
                shiprocketSyncLogService.logSync(
                        order.getId().intValue(),
                        order.getOrderNumber(),
                        shiprocketOrderId,
                        "ORDER_PUSH",
                        status,
                        toJson(requestPayload),
                        toJson(responsePayload),
                        truncate(errorMessage, 4000)
                );
            } catch (Exception e) {
                log.warn("Failed to write ORDER_PUSH sync log for orderId={}: {}", order.getId(), e.getMessage());
            }
        }

        private String toJson(Map<String, Object> payload) {
            if (payload == null || payload.isEmpty()) {
                return null;
            }
            try {
                return truncate(objectMapper.writeValueAsString(payload), 16_000);
            } catch (JsonProcessingException e) {
                return truncate(String.valueOf(payload), 16_000);
            }
        }

        private static String truncate(String value, int max) {
            if (value == null) {
                return null;
            }
            if (value.length() <= max) {
                return value;
            }
            return value.substring(0, max) + "…";
        }

        /**
         * Shiprocket pickup_location = unique nickname for the product's seller warehouse.
         * Never uses Ashvi / ASVI / work platform defaults.
         */
        private String resolvePickupLocation(Long sellerId) {
            if (sellerId == null) {
                throw new RuntimeException(
                        "Order has no seller. Cannot set Shiprocket pickup from seller warehouse."
                );
            }
            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException(
                            "Seller not found for id=" + sellerId
                    ));

            String nickname = sellerBusinessPickupNickname(seller);
            ensureSellerPickupRegistered(seller, nickname);
            log.info(
                    "Shiprocket pickup from product seller sellerId={} businessName={} pickup={}",
                    sellerId,
                    seller.getBusinessName(),
                    nickname
            );
            return nickname;
        }

        private static String sellerBusinessPickupNickname(Seller seller) {
            if (seller == null || seller.getId() == null) {
                throw new RuntimeException("Seller is required for Shiprocket pickup.");
            }
            return ShiprocketPickupSupport.pickupNickname(seller.getId(), seller.getBusinessName());
        }

        private void ensureSellerPickupRegistered(Seller seller, String pickupNickname) {
            if (isBlank(pickupNickname)) {
                return;
            }
            String token = getToken();
            ShiprocketPickupSupport.SellerPickupAddress addr = ShiprocketPickupSupport.buildSellerPickupAddress(
                    seller.getWarehouseAddress(),
                    seller.getWarehouseArea(),
                    seller.getWarehouseCity(),
                    seller.getWarehouseState(),
                    seller.getWarehouseCountry(),
                    seller.getAddress(),
                    null,
                    seller.getCity(),
                    seller.getState(),
                    seller.getCountry(),
                    seller.getPincode()
            );

            if (!addr.isComplete()) {
                throw new RuntimeException(
                        "Seller id=" + seller.getId()
                                + " ('" + pickupNickname + "') needs a complete warehouse/business address "
                                + "(street, city, state, 6-digit PIN) for Shiprocket pickup."
                );
            }

            String phoneDigits = seller.getMobileNumber() != null
                    ? seller.getMobileNumber().replaceAll("[^0-9]", "")
                    : "";
            if (phoneDigits.startsWith("91") && phoneDigits.length() > 10) {
                phoneDigits = phoneDigits.substring(phoneDigits.length() - 10);
            }
            if (phoneDigits.length() != 10) {
                throw new RuntimeException(
                        "Seller '" + pickupNickname + "' needs a valid mobile for Shiprocket pickup."
                );
            }

            Map<String, Object> addPickup = new LinkedHashMap<>();
            addPickup.put("pickup_location", pickupNickname);
            String contactName = firstNonBlank(seller.getBusinessName(), seller.getFirstName(), "Seller");
            addPickup.put("name", contactName.length() > 50
                    ? contactName.substring(0, 50) : contactName);
            addPickup.put("email",
                    !isBlank(seller.getEmail()) ? seller.getEmail().trim() : "support@flintnthread.in");
            addPickup.put("phone", phoneDigits);
            addPickup.put("address", addr.street());
            if (!isBlank(addr.address2())) {
                addPickup.put("address_2", addr.address2());
            }
            addPickup.put("city", addr.city());
            addPickup.put("state", addr.state());
            addPickup.put("country", addr.country());
            addPickup.put("pin_code", addr.pincode());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (pickupExists(token, pickupNickname)) {
                log.info(
                        "Shiprocket pickup exists nickname={} sellerId={} — refreshing address city={} pin={}",
                        pickupNickname, seller.getId(), addr.city(), addr.pincode()
                );
                try {
                    restTemplate.postForEntity(
                            apiBaseUrl + "/settings/company/update/pickup-location",
                            new HttpEntity<>(addPickup, headers),
                            Map.class
                    );
                } catch (Exception updateEx) {
                    log.warn("Shiprocket update pickup skipped nickname={}: {}",
                            pickupNickname, updateEx.getMessage());
                }
                return;
            }

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        apiBaseUrl + "/settings/company/addpickup",
                        new HttpEntity<>(addPickup, headers),
                        Map.class
                );
                log.info("Shiprocket addpickup nickname={} street={} city={} pin={} body={}",
                        pickupNickname, addr.street(), addr.city(), addr.pincode(), response.getBody());
            } catch (HttpClientErrorException e) {
                String apiBody = e.getResponseBodyAsString();
                String lower = (apiBody != null ? apiBody : e.getMessage()).toLowerCase(Locale.ENGLISH);
                if (lower.contains("already") || lower.contains("exist")) {
                    return;
                }
                throw new RuntimeException(
                        "Could not register seller pickup '" + pickupNickname
                                + "' in Shiprocket: " + (apiBody != null && !apiBody.isBlank()
                                ? apiBody : e.getMessage()),
                        e
                );
            }
        }

        private boolean pickupExists(String token, String pickupNickname) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiBaseUrl + "/settings/company/pickup",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );
                Map body = response.getBody();
                List<?> locations = extractPickupList(body);
                for (Object row : locations) {
                    if (!(row instanceof Map<?, ?> map)) {
                        continue;
                    }
                    Object nameObj = map.get("pickup_location");
                    if (nameObj == null) {
                        nameObj = map.get("pickup_location_name");
                    }
                    if (nameObj != null
                            && String.valueOf(nameObj).trim().equalsIgnoreCase(pickupNickname.trim())) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Shiprocket list pickup failed: {}", e.getMessage());
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private static List<?> extractPickupList(Map body) {
            if (body == null || body.isEmpty()) {
                return List.of();
            }
            Object data = body.get("data");
            if (data instanceof Map<?, ?> nested) {
                Object inner = nested.get("data");
                if (inner instanceof List<?> list) {
                    return list;
                }
                if (nested.get("shipping_address") instanceof List<?> list) {
                    return list;
                }
            }
            if (data instanceof List<?> list) {
                return list;
            }
            if (body.get("shipping_address") instanceof List<?> list) {
                return list;
            }
            return List.of();
        }

        private static String firstNonBlank(String... values) {
            if (values == null) {
                return null;
            }
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
            return null;
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

            boolean priceMissing = item.getPrice() == null || item.getPrice() <= 0;
            boolean totalMissing = item.getTotal() == null || item.getTotal() <= 0;
            if ((priceMissing || totalMissing) && variant != null) {
                BigDecimal catalogPrice = firstPositivePrice(
                        variant.getFinalPrice(),
                        variant.getSellingPrice(),
                        variant.getBasePrice()
                );
                if (catalogPrice != null && catalogPrice.compareTo(BigDecimal.ZERO) > 0) {
                    if (priceMissing) {
                        item.setPrice(catalogPrice.doubleValue());
                    }
                    if (totalMissing) {
                        int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                        item.setTotal(catalogPrice.multiply(BigDecimal.valueOf(qty)).doubleValue());
                    }
                }
            }
        }



        private void storeTrackingTimelineFromRemote(Order order, Map<String, Object> remote) {
            try {
                // Extract tracking activities from Shiprocket response
                Object trackingData = firstNode(
                        remote.get("tracking_data"),
                        remote.get("data") instanceof Map<?, ?> ? ((Map<?, ?>) remote.get("data")).get("tracking_data") : null,
                        remote.get("tracking")
                );

                if (!(trackingData instanceof Map<?, ?> tdMap)) {
                    return;
                }

                Object activities = tdMap.get("shipment_track_activities");
                List<?> activityList = null;
                if (activities instanceof List<?> list) {
                    activityList = list;
                } else {
                    activities = tdMap.get("activities");
                    if (activities instanceof List<?> list) {
                        activityList = list;
                    } else {
                        return;
                    }
                }

                if (activityList == null || activityList.isEmpty()) {
                    return;
                }

                // Store each tracking activity as order_status_history entry
                for (Object act : activityList) {
                    if (!(act instanceof Map<?, ?> activity)) {
                        continue;
                    }

                    String status = textOrNull(activity.get("activity"));
                    String location = textOrNull(activity.get("location"));
                    String date = textOrNull(activity.get("date"));
                    
                    if (status == null || status.isBlank()) {
                        continue;
                    }
                    
                    // Check if this status already exists to avoid duplicates
                    boolean alreadyExists = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())
                            .stream()
                            .anyMatch(h -> h.getComment() != null 
                                    && h.getComment().contains("Shiprocket tracking:")
                                    && h.getComment().contains("status=" + status));
                    
                    if (alreadyExists) {
                        continue;
                    }
                    
                    String trackingComment = String.format(
                            "Shiprocket tracking: status=%s, awb=%s, courier=%s, location=%s",
                            status,
                            order.getShiprocketAwbCode() != null ? order.getShiprocketAwbCode() : "",
                            order.getShiprocketCourierName() != null ? order.getShiprocketCourierName() : "",
                            location != null ? location : ""
                    );
                    
                    OrderStatus orderStatusEnum = mapToOrderStatusEnum(status);
                    if (orderStatusEnum != null) {
                        try {
                            OrderStatusHistory history = OrderStatusHistory.builder()
                                    .order(order)
                                    .status(orderStatusEnum)
                                    .comment(trackingComment)
                                    .build();
                            orderStatusHistoryRepository.save(history);
                        } catch (Exception e) {
                            log.warn("Failed to store tracking event for orderId={}: {}", order.getId(), e.getMessage());
                        }
                    }
                }
                
                log.info("Stored {} tracking events for orderNumber={}", activityList.size(), order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Failed to store tracking timeline for orderNumber={}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        private OrderStatus mapToOrderStatusEnum(String status) {
            if (status == null || status.isBlank()) {
                return null;
            }
            String normalized = status.trim().toLowerCase(Locale.ENGLISH).replace("-", "_").replace(" ", "_");
            return switch (normalized) {
                case "new" -> OrderStatus.CREATED;
                case "confirmed", "processing", "packed", "awb_assigned", "pickup_scheduled", "picked_up", "in_transit" 
                    -> OrderStatus.CONFIRMED;
                case "out_for_delivery" -> OrderStatus.OUT_FOR_DELIVERY;
                case "delivered" -> OrderStatus.DELIVERED;
                case "cancelled", "rto_initiated", "rto_delivered" -> OrderStatus.CANCELLED;
                case "returned" -> OrderStatus.RETURNED;
                default -> null;
            };
        }

        private Object firstNode(Object... values) {
            for (Object value : values) {
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        private String textOrNull(Object value) {
            if (value == null) {
                return null;
            }
            String str = value.toString();
            return str.isBlank() ? null : str;
        }

        @Override
        @Transactional
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

                // Store tracking timeline from Shiprocket API response
                storeTrackingTimelineFromRemote(order, remote);

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
                        || "processing".equalsIgnoreCase(mappedStatus)
                        || "shipped".equalsIgnoreCase(mappedStatus)
                        || "delivered".equalsIgnoreCase(mappedStatus)
                        || "returned".equalsIgnoreCase(mappedStatus)) {
                    order.setOrderStatus(mappedStatus);
                }
            } else if (!isBlank(resolvedAwb) && isEarlyFulfillmentStatus(order.getOrderStatus())) {
                order.setShiprocketStatus("processing");
                order.setOrderStatus("processing");
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
                    order.setShiprocketStatus("processing");
                    order.setOrderStatus("processing");
                } else if (!isBlank(resolvedAwb)
                        && isEarlyFulfillmentStatus(order.getOrderStatus())) {
                    order.setShiprocketStatus("processing");
                    order.setOrderStatus("processing");
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

            // Shiprocket often returns numeric shipment_status codes.
            if (normalized.matches("^\\d+$")) {
                return switch (normalized) {
                    case "5", "8", "16", "45" -> "cancelled";
                    case "7", "23", "26" -> "delivered";
                    case "9", "10", "14", "46" -> "returned";
                    case "6", "12", "13", "15", "17", "18", "19", "20", "21", "22",
                            "24", "25", "38", "39", "40", "41", "42", "43" -> "shipped";
                    case "1", "2", "3", "4" -> "processing";
                    default -> "shipped";
                };
            }

            // Return only values allowed by orders.order_status ENUM.
            return switch (normalized) {
                case "new", "confirmed", "processing", "packed", "awb_assigned", "awbassigned",
                        "pickup_scheduled", "pickup_generated", "pickup_queued", "label_generated"
                        -> "processing";
                case "picked_up", "shipped", "in_transit", "intransit", "out_for_delivery", "ofd"
                        -> "shipped";
                case "delivered", "fulfilled", "completed" -> "delivered";
                case "cancelled", "canceled" -> "cancelled";
                case "rto_initiated", "rto_in_transit", "rto_delivered", "return_initiated", "returned"
                        -> "returned";
                default -> {
                    if (normalized.contains("deliver")) yield "delivered";
                    if (normalized.contains("rto") || normalized.contains("return")) yield "returned";
                    if (normalized.contains("cancel")) yield "cancelled";
                    if (normalized.contains("transit") || normalized.contains("ship")
                            || normalized.contains("pick") || normalized.contains("ofd")) yield "shipped";
                    if (normalized.contains("awb") || normalized.contains("process")
                            || normalized.contains("pack")) yield "processing";
                    yield null;
                }
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
        public OrderTrackingResponseDTO getTrackingFromDatabase(Long orderId) {
            if (orderId == null) {
                throw new IllegalArgumentException("Order ID is required for tracking");
            }

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new IllegalArgumentException("Order not found with ID: " + orderId);
            }

            List<OrderTrackingDTO> timeline = new ArrayList<>();
            
            // Fetch tracking events from order_status_history
            List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
            if (history != null) {
                for (OrderStatusHistory entry : history) {
                    String comment = entry.getComment();
                    if (comment != null && comment.contains("Shiprocket tracking:")) {
                        // Parse tracking details from comment
                        OrderTrackingDTO trackingEvent = parseTrackingComment(comment, entry.getCreatedAt());
                        if (trackingEvent != null) {
                            timeline.add(trackingEvent);
                        }
                    }
                }
            }

            // If no tracking events in history but order has Shiprocket data, create initial entry
            if (timeline.isEmpty() && order.getShiprocketStatus() != null) {
                timeline.add(OrderTrackingDTO.builder()
                        .status(order.getShiprocketStatus())
                        .description("Current shipment status")
                        .location("")
                        .timestamp(order.getShiprocketSyncedAt() != null 
                                ? order.getShiprocketSyncedAt() 
                                : order.getUpdatedAt())
                        .build());
            }

            String trackingUrl = order.getShiprocketTrackingUrl();
            if (isBlank(trackingUrl) && !isBlank(order.getShiprocketAwbCode())) {
                trackingUrl = "https://shiprocket.co/tracking/" + order.getShiprocketAwbCode();
            }

            return OrderTrackingResponseDTO.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .awbCode(order.getShiprocketAwbCode())
                    .courierName(order.getShiprocketCourierName() != null 
                            ? order.getShiprocketCourierName() 
                            : "Shiprocket")
                    .trackingUrl(trackingUrl)
                    .currentStatus(order.getShiprocketStatus() != null 
                            ? order.getShiprocketStatus() 
                            : order.getOrderStatus())
                    .timeline(timeline)
                    .build();
        }

        private OrderTrackingDTO parseTrackingComment(String comment, LocalDateTime timestamp) {
            try {
                // Parse format: "Shiprocket tracking: status=XXX, awb=XXX, courier=XXX, location=XXX"
                String status = extractValue(comment, "status=");
                String location = extractValue(comment, "location=");
                String awb = extractValue(comment, "awb=");
                String courier = extractValue(comment, "courier=");

                return OrderTrackingDTO.builder()
                        .status(status != null ? status : "Update")
                        .description(status != null ? status : "Tracking update")
                        .location(location != null ? location : "")
                        .timestamp(timestamp != null ? timestamp : LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to parse tracking comment: {}", comment, e);
                return null;
            }
        }

        private String extractValue(String text, String key) {
            if (text == null || key == null) {
                return null;
            }
            int keyIndex = text.indexOf(key);
            if (keyIndex == -1) {
                return null;
            }
            int startIndex = keyIndex + key.length();
            int endIndex = text.indexOf(',', startIndex);
            if (endIndex == -1) {
                endIndex = text.length();
            }
            String value = text.substring(startIndex, endIndex).trim();
            return value.isEmpty() ? null : value;
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



