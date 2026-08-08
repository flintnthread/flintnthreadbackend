package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.OrderItem;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.shiprocket.ShiprocketOrderPricing;
import com.ecommerce.adminbackend.service.shiprocket.ShiprocketPickupSupport;
import com.ecommerce.adminbackend.service.shiprocket.ShiprocketPushOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates/syncs Shiprocket shipments directly from admin-service
 * (same shared orders DB). Does not depend on user-service being up.
 */
@Service
@RequiredArgsConstructor
public class AdminShiprocketService {

    private static final Logger log = LogFactory.getLogger(AdminShiprocketService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SellerRepository sellerRepository;
    private final PlatformIntegrationSettings integrationSettings;
    private final ShiprocketSyncLogService shiprocketSyncLogService;

    @Value("${shiprocket.api.base-url:https://apiv2.shiprocket.in/v1/external}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20_000);
        factory.setReadTimeout(90_000);
        return new RestTemplate(factory);
    }

    @Transactional
    public Map<String, Object> createOrSyncShipment(Order order) {
        return createOrSyncShipment(order, ShiprocketPushOptions.empty());
    }

    @Transactional
    public Map<String, Object> createOrSyncShipment(Order order, ShiprocketPushOptions pushOptions) {
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Order is required.");
        }
        if (pushOptions == null) {
            pushOptions = ShiprocketPushOptions.empty();
        }

        boolean hasSrOrder = !isBlank(order.getShiprocketOrderId());
        boolean hasAwb = !isBlank(order.getShiprocketAwbCode());
        boolean pushFailed = isFailedPushStatus(order.getShiprocketStatus());

        // Already courier-assigned: only sync, do not recreate.
        if (hasSrOrder && hasAwb) {
            if (isBlank(order.getShiprocketTrackingUrl())) {
                return syncShipment(order);
            }
            Map<String, Object> existing = resultMap(order);
            existing.put("already_exists", true);
            existing.put("message", "Shipment already exists on Shiprocket");
            return existing;
        }

        // Successful create awaiting manual courier assign in Shiprocket — never duplicate.
        if (hasSrOrder && !pushFailed) {
            Map<String, Object> existing = resultMap(order);
            existing.put("already_exists", true);
            existing.put("message",
                    "Shipment already created on Shiprocket. Assign courier in Shiprocket, then Sync Now for AWB.");
            return existing;
        }

        // Prior push failed (pending:…) — clear local link and recreate once.
        if (hasSrOrder && pushFailed) {
            log.warn(
                    "Retrying failed Shiprocket push orderId={} oldSrOrderId={} status={}",
                    order.getId(),
                    order.getShiprocketOrderId(),
                    order.getShiprocketStatus()
            );
            tryCancelShiprocketOrder(order.getShiprocketOrderId());
            clearShiprocketLinkage(order);
            orderRepository.save(order);
        }

        Map<String, Object> payload = null;
        try {
            payload = buildPayload(order, pushOptions);
            Map<String, Object> apiPayload = sanitizePayloadForApi(payload);
            validateCodAmount(order, apiPayload);
            Map<String, Object> body = postCreateAdhoc(apiPayload);
            shiprocketSyncLogService.logPush(
                    order.getId(),
                    order.getOrderNumber(),
                    body.get("order_id") != null ? String.valueOf(body.get("order_id")) : null,
                    "success",
                    apiPayload,
                    body,
                    null
            );
            Map<String, Object> persisted = persistCreateResponse(order, body);
            persisted.put("pickup_location", payload.get("pickup_location"));
            persisted.put("pickup_seller_id", payload.get("_pickup_seller_id"));
            persisted.put("pickup_address", payload.get("_pickup_address"));
            persisted.put("pickup_city", payload.get("_pickup_city"));
            persisted.put("pickup_pincode", payload.get("_pickup_pincode"));
            return persisted;
        } catch (HttpStatusCodeException e) {
            String apiBody = e.getResponseBodyAsString();
            log.error("Shiprocket create failed orderId={} status={} body={}",
                    order.getId(), e.getStatusCode().value(), apiBody);
            shiprocketSyncLogService.logPush(
                    order.getId(),
                    order.getOrderNumber(),
                    null,
                    "failed",
                    payload != null ? sanitizePayloadForApi(payload) : Map.of(),
                    Map.of("httpStatus", e.getStatusCode().value(), "body", apiBody),
                    apiBody
            );
            throw new IllegalStateException(
                    "Shiprocket API error: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        } catch (IllegalStateException e) {
            shiprocketSyncLogService.logPush(
                    order.getId(),
                    order.getOrderNumber(),
                    null,
                    "failed",
                    payload != null ? sanitizePayloadForApi(payload) : Map.of(),
                    null,
                    e.getMessage()
            );
            throw e;
        } catch (Exception e) {
            shiprocketSyncLogService.logPush(
                    order.getId(),
                    order.getOrderNumber(),
                    null,
                    "failed",
                    payload != null ? sanitizePayloadForApi(payload) : Map.of(),
                    null,
                    e.getMessage()
            );
            throw new IllegalStateException(
                    "Shipment creation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e
            );
        }
    }

    /** True when shiprocket_status records a failed push (pending / pending: reason). */
    public static boolean isFailedPushStatus(String shiprocketStatus) {
        if (isBlank(shiprocketStatus)) {
            return false;
        }
        String s = shiprocketStatus.trim().toLowerCase(Locale.ENGLISH);
        return s.equals("pending") || s.startsWith("pending:");
    }

    @Transactional
    public Map<String, Object> syncShipment(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Order is required.");
        }
        boolean hasShipment = !isBlank(order.getShiprocketShipmentId());
        boolean hasSrOrder = !isBlank(order.getShiprocketOrderId());
        boolean hasAwb = !isBlank(order.getShiprocketAwbCode());
        if (!hasShipment && !hasSrOrder && !hasAwb) {
            throw new IllegalStateException("Order is not linked to Shiprocket yet. Push first.");
        }

        try {
            String token = getToken();
            Map<String, Object> merged = new HashMap<>();

            if (hasAwb) {
                Map<?, ?> trackAwb = getJson(token, "/courier/track/awb/" + order.getShiprocketAwbCode().trim());
                mergeDeep(merged, trackAwb);
            }
            if (hasShipment && order.getShiprocketShipmentId().trim().matches("^\\d+$")) {
                Map<?, ?> track = getJson(token, "/courier/track/shipment/" + order.getShiprocketShipmentId().trim());
                mergeDeep(merged, track);
                Map<?, ?> shipment = unwrapData(getJson(token, "/shipments/" + order.getShiprocketShipmentId().trim()));
                mergeDeep(merged, shipment);
            }
            if (hasSrOrder && order.getShiprocketOrderId().trim().matches("^\\d+$")) {
                Map<?, ?> show = unwrapData(getJson(token, "/orders/show/" + order.getShiprocketOrderId().trim()));
                mergeDeep(merged, show);
            }

            String awb = firstString(merged, "awb", "awb_code", "awbCode");
            String courier = firstString(merged, "courier_name", "courier", "sr_courier_name");
            String status = firstString(merged, "current_status", "status", "shipment_status");
            String trackingUrl = firstString(merged, "tracking_url", "track_url", "trackingUrl");
            if (isBlank(trackingUrl) && !isBlank(awb)) {
                trackingUrl = "https://shiprocket.co/tracking/" + awb;
            }

            if (!isBlank(awb)) {
                order.setShiprocketAwbCode(awb.trim().replaceAll("\\.0$", ""));
            }
            if (!isBlank(courier)) {
                order.setShiprocketCourierName(courier);
            }
            if (!isBlank(trackingUrl)) {
                order.setShiprocketTrackingUrl(trackingUrl);
            }
            if (!isBlank(status)) {
                order.setShiprocketStatus(trimStatus(status));
            } else if (!isBlank(order.getShiprocketAwbCode())) {
                order.setShiprocketStatus("processing");
            }
            order.setShiprocketSyncedAt(LocalDateTime.now());

            // Keep shop order status in sync with Shiprocket logistics (separate from payment).
            String mappedOrderStatus = mapShiprocketToOrderStatus(
                    order.getShiprocketStatus(),
                    order.getShiprocketAwbCode()
            );
            if (!isBlank(mappedOrderStatus)) {
                String current = order.getOrderStatus() != null
                        ? order.getOrderStatus().trim().toLowerCase(Locale.ENGLISH)
                        : "";
                if (!current.contains("cancel")
                        && !"delivered".equals(current)
                        && !"completed".equals(current)) {
                    order.setOrderStatus(mappedOrderStatus);
                }
            }

            orderRepository.save(order);

            Map<String, Object> out = resultMap(order);
            out.put("message", "Shiprocket shipment synced");
            return out;
        } catch (HttpStatusCodeException e) {
            String apiBody = e.getResponseBodyAsString();
            throw new IllegalStateException(
                    "Shiprocket sync error: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Shiprocket sync failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e
            );
        }
    }

    private Map<String, Object> persistCreateResponse(Order order, Map<String, Object> body) {
        String shiprocketOrderId = body.get("order_id") != null ? String.valueOf(body.get("order_id")) : null;
        String shipmentId = null;
        Object shipmentObj = body.get("shipment_id");
        if (shipmentObj instanceof List<?> list && !list.isEmpty()) {
            shipmentId = String.valueOf(list.get(0));
        } else if (shipmentObj != null) {
            shipmentId = String.valueOf(shipmentObj);
        }
        if ((shipmentId == null || shipmentId.isBlank() || "null".equalsIgnoreCase(shipmentId))
                && body.get("shipment_ids") instanceof List<?> ids && !ids.isEmpty()) {
            shipmentId = String.valueOf(ids.get(0));
        }

        if (isBlank(shiprocketOrderId) || "null".equalsIgnoreCase(shiprocketOrderId)) {
            throw new IllegalStateException("Shiprocket did not return order_id. Response: " + body);
        }

        String awb = body.get("awb_code") != null
                && !"null".equalsIgnoreCase(String.valueOf(body.get("awb_code")))
                && !String.valueOf(body.get("awb_code")).isBlank()
                ? String.valueOf(body.get("awb_code"))
                : null;
        String courier = body.get("courier_name") != null
                && !"null".equalsIgnoreCase(String.valueOf(body.get("courier_name")))
                && !String.valueOf(body.get("courier_name")).isBlank()
                ? String.valueOf(body.get("courier_name"))
                : null;

        // Do not call /courier/assign/awb — courier is assigned manually in Shiprocket.

        String trackingUrl = !isBlank(awb) ? "https://shiprocket.co/tracking/" + awb : null;
        String status = !isBlank(awb) ? "awb_assigned" : "awaiting_courier";

        order.setShiprocketOrderId(shiprocketOrderId);
        order.setShiprocketShipmentId(shipmentId);
        order.setShiprocketAwbCode(awb);
        order.setShiprocketCourierName(courier);
        order.setShiprocketTrackingUrl(trackingUrl);
        order.setShiprocketStatus(status);
        order.setShiprocketPushedAt(LocalDateTime.now());
        order.setShiprocketSyncedAt(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> out = resultMap(order);
        out.put("message", !isBlank(awb)
                ? "Shipment created on Shiprocket"
                : "Shipment created on Shiprocket. Assign courier in Shiprocket, then Sync Now.");
        return out;
    }

    private Map<String, Object> buildPayload(Order order, ShiprocketPushOptions pushOptions) {
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        if (allItems == null || allItems.isEmpty()) {
            throw new IllegalStateException("No order items found for Shiprocket.");
        }

        Map<Long, Long> productIdByVariantId = resolveProductIdsByVariantId(allItems);
        Map<Long, Product> productsById = loadProducts(allItems, productIdByVariantId);
        Map<Long, ProductVariant> variantsById = loadVariants(allItems);
        Map<Long, Seller> sellersById = loadSellers(allItems, productsById, productIdByVariantId);

        List<OrderItem> items = filterItemsForPush(
                allItems,
                productIdByVariantId,
                productsById,
                sellersById,
                pushOptions
        );
        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "No order items match the selected seller/products for Shiprocket push."
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
            Long effectiveProductId = resolveEffectiveProductId(item, productIdByVariantId);
            Product product = effectiveProductId != null ? productsById.get(effectiveProductId) : null;
            ProductVariant variant = item.getVariantId() != null ? variantsById.get(item.getVariantId()) : null;

            Long itemSellerId = ShiprocketPickupSupport.resolveSellerId(
                    product != null ? product.getSellerId() : null,
                    item.getSellerId()
            );
            if (primarySellerId == null) {
                primarySellerId = itemSellerId;
            }

            String name = product != null && !isBlank(product.getName())
                    ? product.getName()
                    : "Product";
            String sku = product != null && !isBlank(product.getSku())
                    ? product.getSku()
                    : (variant != null && !isBlank(variant.getSku())
                    ? variant.getSku()
                    : "SKU-" + (effectiveProductId != null ? effectiveProductId : item.getId()));
            String hsn = product != null && !isBlank(product.getHsnCode())
                    ? product.getHsnCode()
                    : "0000";
            int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;

            BigDecimal unitPrice = item.getPrice();
            BigDecimal lineTotal = item.getTotal();
            if ((unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0)
                    && (lineTotal == null || lineTotal.compareTo(BigDecimal.ZERO) <= 0)
                    && variant != null) {
                unitPrice = firstPositivePrice(
                        variant.getFinalPrice(),
                        variant.getSellingPrice(),
                        variant.getBasePrice()
                );
                if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
                    lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                }
            }
            scopedItemTotal = scopedItemTotal.add(
                    lineTotal != null && lineTotal.compareTo(BigDecimal.ZERO) > 0
                            ? lineTotal
                            : (unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO)
            );

            pricingLines.add(new ShiprocketOrderPricing.LineInput(
                    name,
                    sku,
                    hsn,
                    qty,
                    unitPrice,
                    lineTotal
            ));

            double weight = product != null && product.getProductWeight() != null
                    ? product.getProductWeight().doubleValue()
                    : 0.5;
            totalWeight += weight * qty;

            if (product != null) {
                if (product.getLengthCm() != null) {
                    maxLength = Math.max(maxLength, product.getLengthCm().doubleValue());
                }
                if (product.getWidthCm() != null) {
                    maxWidth = Math.max(maxWidth, product.getWidthCm().doubleValue());
                }
                if (product.getHeightCm() != null) {
                    maxHeight = Math.max(maxHeight, product.getHeightCm().doubleValue());
                }
            }
        }

        BigDecimal orderAmountForPricing = scopedPush
                ? scopedItemTotal
                : order.getTotalAmount();
        if (scopedPush && orderAmountForPricing.compareTo(BigDecimal.ZERO) <= 0) {
            orderAmountForPricing = scopedItemTotal;
        }

        ShiprocketOrderPricing.PricedPayload priced = ShiprocketOrderPricing.build(
                pricingLines,
                orderAmountForPricing,
                order.getShippingAmount(),
                scopedPush ? BigDecimal.ZERO : order.getDiscountAmount(),
                scopedPush ? BigDecimal.ZERO : order.getReferralDiscountAmount()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        String shiprocketOrderRef = order.getOrderNumber();
        if (scopedPush && primarySellerId != null) {
            shiprocketOrderRef = order.getOrderNumber() + "-S" + primarySellerId;
        }
        payload.put("order_id", shiprocketOrderRef);
        payload.put("order_date", LocalDate.now().toString());

        Seller seller = requireSellerForPickup(primarySellerId);
        String pickupNickname = sellerBusinessPickupNickname(seller);
        ShiprocketPickupSupport.SellerPickupAddress pickupAddr = ShiprocketPickupSupport.buildSellerPickupAddress(
                seller.getWarehouseAddress(),
                seller.getWarehouseArea(),
                seller.getWarehouseCity(),
                seller.getWarehouseState(),
                seller.getWarehouseCountry(),
                seller.getAddress(),
                seller.getArea(),
                seller.getCity(),
                seller.getState(),
                seller.getCountry(),
                seller.getPincode()
        );
        ensureSellerPickupRegistered(seller, pickupNickname);
        payload.put("pickup_location", pickupNickname);
        payload.put("_pickup_seller_id", seller.getId());
        payload.put("_pickup_address", pickupAddr.street());
        payload.put("_pickup_city", pickupAddr.city());
        payload.put("_pickup_pincode", pickupAddr.pincode());
        log.info(
                "Shiprocket pickup resolved orderId={} sellerId={} businessName={} pickup={} address={} city={} pin={} scoped={}",
                order.getId(),
                seller.getId(),
                seller.getBusinessName(),
                pickupNickname,
                pickupAddr.street(),
                pickupAddr.city(),
                pickupAddr.pincode(),
                scopedPush
        );

        String[] nameParts = splitCustomerName(order.getShippingName());
        payload.put("billing_customer_name", nameParts[0]);
        payload.put("billing_last_name", nameParts[1]);
        payload.put("billing_phone", normalizeIndianMobile(order.getShippingPhone()));
        payload.put("billing_email",
                !isBlank(order.getShippingEmail()) ? order.getShippingEmail() : "support@flintnthread.in");

        String address = order.getShippingAddress1();
        if (!isBlank(order.getShippingAddress2())) {
            address = (address != null ? address + ", " : "") + order.getShippingAddress2();
        }
        if (isBlank(address)) {
            address = "Address not provided";
        }
        payload.put("billing_address", address);
        payload.put("billing_city",
                !isBlank(order.getShippingCity()) ? order.getShippingCity().trim() : "Hyderabad");
        payload.put("billing_state",
                !isBlank(order.getShippingState()) ? order.getShippingState().trim() : "Telangana");
        String pincode = order.getShippingPincode() != null
                ? order.getShippingPincode().replaceAll("[^0-9]", "")
                : "";
        if (pincode.length() != 6) {
            throw new IllegalStateException(
                    "Invalid shipping pincode for Shiprocket: '" + order.getShippingPincode()
                            + "'. Need a valid 6-digit PIN."
            );
        }
        payload.put("billing_pincode", pincode);
        payload.put("billing_country", "India");
        payload.put("shipping_is_billing", true);
        payload.put("order_items", priced.orderItems());
        payload.put("payment_method", isCod(order.getPaymentMethod()) ? "COD" : "Prepaid");
        payload.put("sub_total", priced.subTotal().doubleValue());
        payload.put("shipping_charges", priced.shippingCharges().doubleValue());
        payload.put("total_discount", priced.totalDiscount().doubleValue());
        payload.put("grand_total", ShiprocketOrderPricing.computeGrandTotal(priced).doubleValue());
        log.info(
                "Shiprocket pricing orderNumber={} scoped={} orderPlacingTotal={} shiprocketSubTotal={} grandTotal={} shippingCharges={} discount={}",
                order.getOrderNumber(),
                scopedPush,
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
        payload.put("comment", "FNT Order " + order.getOrderNumber()
                + (scopedPush ? " seller " + seller.getBusinessName() : ""));
        return payload;
    }

    private Map<String, Object> postCreateAdhoc(Map<String, Object> payload) {
        String token = getToken();
        HttpHeaders headers = authHeaders(token);
        log.info("Shiprocket create/adhoc pickup={} phone={}",
                payload.get("pickup_location"), payload.get("billing_phone"));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiBaseUrl + "/orders/create/adhoc",
                new HttpEntity<>(payload, headers),
                Map.class
        );
        Map body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty Shiprocket response");
        }
        log.info("Shiprocket create response: {}", body);

        String message = body.get("message") != null ? String.valueOf(body.get("message")) : "";
        if (message.toLowerCase(Locale.ENGLISH).contains("wrong pickup")
                || message.toLowerCase(Locale.ENGLISH).contains("pickup location")) {
            throw new IllegalStateException(
                    "Shiprocket rejected pickup '" + payload.get("pickup_location")
                            + "'. Register this seller business name as a pickup location in Shiprocket. "
                            + message
            );
        }

        Object statusCode = body.get("status_code");
        if (statusCode != null) {
            int code;
            try {
                code = Integer.parseInt(String.valueOf(statusCode));
            } catch (NumberFormatException ex) {
                code = -1;
            }
            if (code != 1 && code != 200) {
                throw new IllegalStateException(
                        "Shiprocket rejected order: " + (!message.isBlank() ? message : body)
                );
            }
        }

        Object orderId = body.get("order_id");
        if (orderId == null || String.valueOf(orderId).isBlank()
                || "null".equalsIgnoreCase(String.valueOf(orderId))) {
            throw new IllegalStateException(
                    "Shiprocket did not return order_id. Response: " + body
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> casted = (Map<String, Object>) body;
        return casted;
    }

    private Seller requireSellerForPickup(Long sellerId) {
        if (sellerId == null) {
            throw new IllegalStateException(
                    "Order has no seller. Cannot set Shiprocket pickup from seller business address."
            );
        }
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Seller not found for id=" + sellerId
                                + ". Cannot set Shiprocket pickup location."
                ));
    }

    /** Shiprocket pickup nickname unique to this seller (never Ashvi/work defaults). */
    private static String sellerBusinessPickupNickname(Seller seller) {
        if (seller == null || seller.getId() == null) {
            throw new IllegalStateException(
                    "Seller is required for Shiprocket pickup."
            );
        }
        return ShiprocketPickupSupport.pickupNickname(seller.getId(), seller.getBusinessName());
    }

    /**
     * Ensure seller pickup exists on Shiprocket, created from the product seller warehouse address.
     */
    private void ensureSellerPickupRegistered(Seller seller, String pickupNickname) {
        String token = getToken();
        ShiprocketPickupSupport.SellerPickupAddress addr = ShiprocketPickupSupport.buildSellerPickupAddress(
                seller.getWarehouseAddress(),
                seller.getWarehouseArea(),
                seller.getWarehouseCity(),
                seller.getWarehouseState(),
                seller.getWarehouseCountry(),
                seller.getAddress(),
                seller.getArea(),
                seller.getCity(),
                seller.getState(),
                seller.getCountry(),
                seller.getPincode()
        );

        if (!addr.isComplete()) {
            throw new IllegalStateException(
                    "Seller id=" + seller.getId()
                            + " ('" + pickupNickname + "') needs a complete warehouse/business address "
                            + "(street, city, state, 6-digit PIN) before Push to Shiprocket."
            );
        }

        String phoneRaw = seller.getMobile();
        String phone;
        try {
            phone = normalizeIndianMobile(phoneRaw);
        } catch (IllegalStateException ex) {
            throw new IllegalStateException(
                    "Seller '" + pickupNickname + "' needs a valid mobile for Shiprocket pickup.",
                    ex
            );
        }

        String contactName = firstNonBlank(
                seller.getBusinessName(),
                seller.getFirstName(),
                "Seller"
        );
        String email = !isBlank(seller.getEmail()) ? seller.getEmail().trim() : "support@flintnthread.in";

        Map<String, Object> addPickup = new LinkedHashMap<>();
        addPickup.put("pickup_location", pickupNickname);
        addPickup.put("name", contactName.length() > 50 ? contactName.substring(0, 50) : contactName);
        addPickup.put("email", email);
        addPickup.put("phone", phone);
        addPickup.put("address", addr.street());
        if (!isBlank(addr.address2())) {
            addPickup.put("address_2", addr.address2());
        }
        addPickup.put("city", addr.city());
        addPickup.put("state", addr.state());
        addPickup.put("country", addr.country());
        addPickup.put("pin_code", addr.pincode());

        if (pickupExists(token, pickupNickname)) {
            log.info(
                    "Shiprocket pickup exists nickname={} sellerId={} — refreshing address city={} pin={}",
                    pickupNickname, seller.getId(), addr.city(), addr.pincode()
            );
            tryUpdateSellerPickup(token, addPickup, pickupNickname);
            return;
        }

        log.info("Registering Shiprocket pickup nickname={} sellerId={} street={} city={} pin={}",
                pickupNickname, seller.getId(), addr.street(), addr.city(), addr.pincode());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiBaseUrl + "/settings/company/addpickup",
                    new HttpEntity<>(addPickup, authHeaders(token)),
                    Map.class
            );
            log.info("Shiprocket addpickup response nickname={} body={}",
                    pickupNickname, response.getBody());
        } catch (HttpStatusCodeException e) {
            String apiBody = e.getResponseBodyAsString();
            String lower = (apiBody != null ? apiBody : e.getMessage()).toLowerCase(Locale.ENGLISH);
            if (lower.contains("already") || lower.contains("exist")) {
                log.info("Shiprocket pickup already present nickname={}", pickupNickname);
                tryUpdateSellerPickup(token, addPickup, pickupNickname);
                return;
            }
            throw new IllegalStateException(
                    "Could not register seller pickup '" + pickupNickname
                            + "' in Shiprocket: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        }

        if (!pickupExists(token, pickupNickname)) {
            throw new IllegalStateException(
                    "Shiprocket pickup '" + pickupNickname
                            + "' was submitted but is not visible yet. Wait a moment and Retry Push."
            );
        }
    }

    /** Best-effort refresh of seller warehouse address on an existing Shiprocket pickup nickname. */
    private void tryUpdateSellerPickup(String token, Map<String, Object> addPickup, String pickupNickname) {
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiBaseUrl + "/settings/company/update/pickup-location",
                    new HttpEntity<>(addPickup, authHeaders(token)),
                    Map.class
            );
            log.info("Shiprocket update pickup nickname={} body={}", pickupNickname, response.getBody());
        } catch (Exception e) {
            log.warn("Shiprocket update pickup skipped nickname={}: {}", pickupNickname, e.getMessage());
        }
    }

    private void tryCancelShiprocketOrder(String shiprocketOrderId) {
        cancelRemoteShipment(shiprocketOrderId);
    }

    /**
     * Cancel a linked Shiprocket order (admin Mark as Cancelled / recreate cleanup).
     * @return true when cancel API succeeded or there was nothing to cancel
     */
    public boolean cancelRemoteShipment(String shiprocketOrderId) {
        if (isBlank(shiprocketOrderId) || !shiprocketOrderId.trim().matches("^\\d+$")) {
            return true;
        }
        try {
            String token = getToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ids", List.of(Long.parseLong(shiprocketOrderId.trim())));
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiBaseUrl + "/orders/cancel",
                    new HttpEntity<>(body, authHeaders(token)),
                    Map.class
            );
            log.info("Shiprocket cancel orderId={} body={}",
                    shiprocketOrderId, response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Shiprocket cancel failed orderId={}: {}",
                    shiprocketOrderId, e.getMessage());
            return false;
        }
    }

    private static void clearShiprocketLinkage(Order order) {
        order.setShiprocketOrderId(null);
        order.setShiprocketShipmentId(null);
        order.setShiprocketAwbCode(null);
        order.setShiprocketCourierName(null);
        order.setShiprocketTrackingUrl(null);
        order.setShiprocketStatus(null);
        order.setShiprocketPushedAt(null);
        order.setShiprocketSyncedAt(null);
    }

    private boolean pickupExists(String token, String pickupNickname) {
        Map<?, ?> body = getJson(token, "/settings/company/pickup");
        List<?> locations = extractPickupList(body);
        for (Object row : locations) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            String name = firstString(map, "pickup_location", "pickup_location_name", "name");
            if (!isBlank(name) && name.trim().equalsIgnoreCase(pickupNickname.trim())) {
                return true;
            }
        }
        return false;
    }

    private static List<?> extractPickupList(Map<?, ?> body) {
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
            if (!isBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private String getToken() {
        String email = integrationSettings.getShiprocketEmail();
        String password = integrationSettings.getShiprocketPassword();
        if (isBlank(email) || isBlank(password)) {
            throw new IllegalStateException(
                    "Shiprocket credentials missing. Set them in Admin → Platform Settings."
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email.trim());
        body.put("password", password);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiBaseUrl + "/auth/login",
                new HttpEntity<>(body, headers),
                Map.class
        );
        if (response.getBody() != null && response.getBody().get("token") != null) {
            return String.valueOf(response.getBody().get("token"));
        }
        throw new IllegalStateException("Shiprocket token failed: " + response.getBody());
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> getJson(String token, String path) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + path,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)),
                    Map.class
            );
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (Exception e) {
            log.warn("Shiprocket GET {} failed: {}", path, e.getMessage());
            return Map.of();
        }
    }

    private static Map<?, ?> unwrapData(Map<?, ?> body) {
        if (body == null || body.isEmpty()) {
            return Map.of();
        }
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            return map;
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static void mergeDeep(Map<String, Object> target, Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<?, ?> e : source.entrySet()) {
            if (e.getKey() != null) {
                target.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        Object trackingData = source.get("tracking_data");
        if (trackingData instanceof Map<?, ?> td) {
            mergeDeep(target, td);
            Object shipmentTrack = td.get("shipment_track");
            if (shipmentTrack instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?> first) {
                mergeDeep(target, first);
            }
        }
    }

    private static Map<String, Object> resultMap(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shipment_id", order.getShiprocketShipmentId());
        m.put("awb_code", order.getShiprocketAwbCode());
        m.put("tracking_url", order.getShiprocketTrackingUrl());
        m.put("courier_name", order.getShiprocketCourierName());
        m.put("order_id", order.getShiprocketOrderId());
        m.put("status", order.getShiprocketStatus());
        return m;
    }

    private static String[] splitCustomerName(String fullName) {
        if (isBlank(fullName)) {
            return new String[]{"Customer", ""};
        }
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int space = trimmed.indexOf(' ');
        if (space > 0) {
            return new String[]{trimmed.substring(0, space), trimmed.substring(space + 1)};
        }
        return new String[]{trimmed, ""};
    }

    private static String normalizeIndianMobile(String rawPhone) {
        if (isBlank(rawPhone)) {
            throw new IllegalStateException(
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
        if (digits.length() != 10 || !digits.matches("^[6-9].*")) {
            throw new IllegalStateException(
                    "Invalid shipping phone for Shiprocket: '" + rawPhone
                            + "'. Need a valid 10-digit Indian mobile."
            );
        }
        return digits;
    }

    private static boolean isCod(String paymentMethod) {
        if (isBlank(paymentMethod)) {
            return false;
        }
        String m = paymentMethod.trim().toLowerCase(Locale.ENGLISH);
        return m.equals("cod") || m.contains("cash on delivery") || m.contains("cash_on_delivery");
    }

    private static String firstString(Map<?, ?> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        // shallow search nested maps
        for (Object value : map.values()) {
            if (value instanceof Map<?, ?> nested) {
                String found = firstString(nested, keys);
                if (!isBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String stringVal(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private static String trimStatus(String status) {
        if (status == null) {
            return null;
        }
        return status.length() > 500 ? status.substring(0, 500) : status;
    }

    /**
     * Map Shiprocket logistics status → shop order_status (never invent shipped without AWB).
     */
    public static String mapShiprocketToOrderStatus(String shiprocketStatus, String awb) {
        String s = shiprocketStatus != null ? shiprocketStatus.trim().toLowerCase(Locale.ENGLISH) : "";
        // Numeric Shiprocket shipment_status codes
        if (s.matches("^\\d+$")) {
            return switch (s) {
                case "5", "8", "16", "45" -> "cancelled";
                case "7", "23", "26" -> "delivered";
                case "9", "10", "14", "46" -> "returned";
                case "6", "12", "13", "15", "17", "18", "19", "20", "21", "22",
                        "24", "25", "38", "39", "40", "41", "42", "43" -> "shipped";
                case "1", "2", "3", "4" -> "processing";
                default -> "shipped";
            };
        }
        if (s.contains("cancel")) {
            return "cancelled";
        }
        if (s.contains("deliver")) {
            return "delivered";
        }
        if (s.contains("rto") || s.contains("return")) {
            return "returned";
        }
        if (s.contains("out for delivery") || s.contains("out_for_delivery")
                || s.contains("in transit") || s.contains("in_transit")
                || s.contains("shipped") || s.contains("picked") || s.contains("pickup")) {
            return "shipped";
        }
        if (!isBlank(awb) || s.contains("awb") || s.contains("process") || s.contains("label")) {
            return "processing";
        }
        // Shipment created, courier not assigned yet — keep processing.
        return null;
    }

    /**
     * Fetch the real Shiprocket shipping-label PDF (requires AWB assigned in Shiprocket).
     */
    public byte[] fetchShippingLabelPdf(Order order) {
        if (order == null) {
            throw new IllegalStateException("Order is required.");
        }
        if (isBlank(order.getShiprocketAwbCode())) {
            throw new IllegalStateException(
                    "AWB is not assigned yet. Assign courier in Shiprocket, then Sync Now before downloading the label."
            );
        }
        if (isBlank(order.getShiprocketShipmentId())
                || !order.getShiprocketShipmentId().trim().matches("^\\d+$")) {
            throw new IllegalStateException(
                    "Shiprocket shipment id missing. Sync the order from Shiprocket first."
            );
        }

        String token = getToken();
        long shipmentId = Long.parseLong(order.getShiprocketShipmentId().trim());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shipment_id", List.of(shipmentId));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/courier/generate/label",
                    HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders(token)),
                    Map.class
            );
            Map<?, ?> resp = response.getBody() != null ? response.getBody() : Map.of();
            String labelUrl = firstString(resp, "label_url", "labelUrl");
            if (isBlank(labelUrl)) {
                throw new IllegalStateException(
                        "Shiprocket did not return label_url. Response: " + resp
                );
            }
            ResponseEntity<byte[]> pdf = restTemplate.exchange(
                    labelUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    byte[].class
            );
            byte[] bytes = pdf.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("Shiprocket label PDF download was empty.");
            }
            return bytes;
        } catch (IllegalStateException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            String apiBody = e.getResponseBodyAsString();
            throw new IllegalStateException(
                    "Shiprocket label API error: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not download Shiprocket label: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e
            );
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        if (!isCod(order.getPaymentMethod())) {
            return;
        }
        double subTotal = toDouble(apiPayload.get("sub_total"));
        double grandTotal = toDouble(apiPayload.get("grand_total"));
        if (subTotal <= 0 && grandTotal <= 0) {
            throw new IllegalStateException(
                    "COD order amount is zero. Check order item prices/totals before pushing to Shiprocket."
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

    private List<OrderItem> filterItemsForPush(
            List<OrderItem> allItems,
            Map<Long, Long> productIdByVariantId,
            Map<Long, Product> productsById,
            Map<Long, Seller> sellersById,
            ShiprocketPushOptions pushOptions
    ) {
        if (pushOptions == null || !pushOptions.isScoped()) {
            return allItems;
        }
        Set<Long> productFilter = new HashSet<>(pushOptions.productIds());
        String sellerNameFilter = pushOptions.normalizedSellerName();
        return allItems.stream()
                .filter(item -> matchesPushScope(
                        item,
                        productIdByVariantId,
                        productsById,
                        sellersById,
                        pushOptions.sellerId(),
                        productFilter,
                        sellerNameFilter
                ))
                .collect(Collectors.toList());
    }

    private boolean matchesPushScope(
            OrderItem item,
            Map<Long, Long> productIdByVariantId,
            Map<Long, Product> productsById,
            Map<Long, Seller> sellersById,
            Long sellerIdFilter,
            Set<Long> productFilter,
            String sellerNameFilter
    ) {
        Long effectiveProductId = resolveEffectiveProductId(item, productIdByVariantId);
        Product product = effectiveProductId != null ? productsById.get(effectiveProductId) : null;
        Long itemSellerId = ShiprocketPickupSupport.resolveSellerId(
                product != null ? product.getSellerId() : null,
                item.getSellerId()
        );

        if (!productFilter.isEmpty()) {
            return (effectiveProductId != null && productFilter.contains(effectiveProductId))
                    || (item.getId() != null && productFilter.contains(item.getId().longValue()));
        }
        if (sellerIdFilter != null) {
            return sellerIdFilter.equals(itemSellerId);
        }
        if (sellerNameFilter != null) {
            Seller seller = itemSellerId != null ? sellersById.get(itemSellerId) : null;
            if (seller == null || isBlank(seller.getBusinessName())) {
                return false;
            }
            return seller.getBusinessName().trim().toLowerCase(Locale.ENGLISH).equals(sellerNameFilter);
        }
        return true;
    }

    private Map<Long, Long> resolveProductIdsByVariantId(List<OrderItem> items) {
        Set<Long> variantIds = items.stream()
                .map(OrderItem::getVariantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> productIdByVariantId = new HashMap<>();
        for (ProductVariant variant : productVariantRepository.findAllById(variantIds)) {
            if (variant.getId() != null && variant.getProductId() != null) {
                productIdByVariantId.put(variant.getId(), variant.getProductId());
            }
        }
        return productIdByVariantId;
    }

    private Map<Long, Product> loadProducts(List<OrderItem> items, Map<Long, Long> productIdByVariantId) {
        Set<Long> productIds = new HashSet<>();
        for (OrderItem item : items) {
            Long productId = resolveEffectiveProductId(item, productIdByVariantId);
            if (productId != null) {
                productIds.add(productId);
            }
        }
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, ProductVariant> loadVariants(List<OrderItem> items) {
        Set<Long> variantIds = items.stream()
                .map(OrderItem::getVariantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return productVariantRepository.findAllById(variantIds).stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, v -> v, (a, b) -> a));
    }

    private Map<Long, Seller> loadSellers(
            List<OrderItem> items,
            Map<Long, Product> productsById,
            Map<Long, Long> productIdByVariantId
    ) {
        Set<Long> sellerIds = new HashSet<>();
        for (OrderItem item : items) {
            Long productId = resolveEffectiveProductId(item, productIdByVariantId);
            Product product = productId != null ? productsById.get(productId) : null;
            Long sellerId = ShiprocketPickupSupport.resolveSellerId(
                    product != null ? product.getSellerId() : null,
                    item.getSellerId()
            );
            if (sellerId != null) {
                sellerIds.add(sellerId);
            }
        }
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        return sellerRepository.findAllById(sellerIds).stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(Seller::getId, s -> s, (a, b) -> a));
    }

    private static Long resolveEffectiveProductId(OrderItem item, Map<Long, Long> productIdByVariantId) {
        if (item.getProductId() != null) {
            return item.getProductId();
        }
        if (item.getVariantId() != null) {
            return productIdByVariantId.get(item.getVariantId());
        }
        return null;
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
}
