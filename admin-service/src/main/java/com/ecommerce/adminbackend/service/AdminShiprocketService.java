package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.OrderItem;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final SellerRepository sellerRepository;
    private final PlatformIntegrationSettings integrationSettings;

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
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Order is required.");
        }

        if (order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank()) {
            if (isBlank(order.getShiprocketAwbCode()) || isBlank(order.getShiprocketTrackingUrl())) {
                return syncShipment(order);
            }
            Map<String, Object> existing = resultMap(order);
            existing.put("already_exists", true);
            existing.put("message", "Shipment already exists on Shiprocket");
            return existing;
        }

        try {
            Map<String, Object> payload = buildPayload(order);
            Map<String, Object> body = postCreateAdhoc(payload);
            return persistCreateResponse(order, body);
        } catch (HttpStatusCodeException e) {
            String apiBody = e.getResponseBodyAsString();
            log.error("Shiprocket create failed orderId={} status={} body={}",
                    order.getId(), e.getStatusCode().value(), apiBody);
            throw new IllegalStateException(
                    "Shiprocket API error: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Shipment creation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e
            );
        }
    }

    @Transactional
    public Map<String, Object> syncShipment(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Order is required.");
        }
        if (isBlank(order.getShiprocketOrderId()) && isBlank(order.getShiprocketShipmentId())) {
            throw new IllegalStateException("Order is not linked to Shiprocket yet. Push first.");
        }

        try {
            String token = getToken();
            Map<String, Object> merged = new HashMap<>();

            if (!isBlank(order.getShiprocketShipmentId())
                    && order.getShiprocketShipmentId().trim().matches("^\\d+$")) {
                Map<?, ?> track = getJson(token, "/courier/track/shipment/" + order.getShiprocketShipmentId().trim());
                mergeDeep(merged, track);
                Map<?, ?> shipment = unwrapData(getJson(token, "/shipments/" + order.getShiprocketShipmentId().trim()));
                mergeDeep(merged, shipment);
            }
            if (!isBlank(order.getShiprocketOrderId())
                    && order.getShiprocketOrderId().trim().matches("^\\d+$")) {
                Map<?, ?> show = unwrapData(getJson(token, "/orders/show/" + order.getShiprocketOrderId().trim()));
                mergeDeep(merged, show);
            }

            String awb = firstString(merged, "awb", "awb_code", "awbCode");
            String courier = firstString(merged, "courier_name", "courier", "sr_courier_name");
            String status = firstString(merged, "status", "current_status", "shipment_status");
            String trackingUrl = firstString(merged, "tracking_url", "track_url", "trackingUrl");
            if (isBlank(trackingUrl) && !isBlank(awb)) {
                trackingUrl = "https://shiprocket.co/tracking/" + awb;
            }

            if (!isBlank(awb)) {
                order.setShiprocketAwbCode(awb);
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
                order.setShiprocketStatus("awb_assigned");
            }
            order.setShiprocketSyncedAt(LocalDateTime.now());
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
        String status = !isBlank(awb) ? "awb_assigned" : "new";

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

    private Map<String, Object> buildPayload(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("No order items found for Shiprocket.");
        }

        List<Map<String, Object>> orderItems = new ArrayList<>();
        double totalWeight = 0;
        double maxLength = 1;
        double maxWidth = 1;
        double maxHeight = 1;
        Long primarySellerId = null;

        for (OrderItem item : items) {
            Product product = item.getProductId() != null
                    ? productRepository.findById(item.getProductId()).orElse(null)
                    : null;
            if (primarySellerId == null) {
                primarySellerId = item.getSellerId() != null
                        ? item.getSellerId()
                        : (product != null ? product.getSellerId() : null);
            }

            String name = product != null && !isBlank(product.getName())
                    ? product.getName()
                    : "Product";
            String sku = product != null && !isBlank(product.getSku())
                    ? product.getSku()
                    : "SKU-" + (item.getProductId() != null ? item.getProductId() : item.getId());
            String hsn = product != null && !isBlank(product.getHsnCode())
                    ? product.getHsnCode()
                    : "0000";

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("name", name);
            line.put("sku", sku);
            line.put("units", item.getQuantity() != null ? item.getQuantity() : 1);
            line.put("selling_price", item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
            line.put("hsn", hsn);
            orderItems.add(line);

            double weight = product != null && product.getProductWeight() != null
                    ? product.getProductWeight().doubleValue()
                    : 0.5;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("order_id", order.getOrderNumber());
        payload.put("order_date", LocalDate.now().toString());

        Seller seller = requireSellerForPickup(primarySellerId);
        String pickupNickname = sellerBusinessPickupNickname(seller);
        ensureSellerPickupRegistered(seller, pickupNickname);
        payload.put("pickup_location", pickupNickname);

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
        payload.put("order_items", orderItems);
        payload.put("payment_method", isCod(order.getPaymentMethod()) ? "COD" : "Prepaid");
        payload.put("sub_total", order.getTotalAmount() != null ? order.getTotalAmount() : 0);
        payload.put("shipping_charges", order.getShippingAmount() != null ? order.getShippingAmount() : 0);
        payload.put("length", Math.max(maxLength, 1));
        payload.put("breadth", Math.max(maxWidth, 1));
        payload.put("height", Math.max(maxHeight, 1));
        payload.put("weight", totalWeight > 0 ? totalWeight : 0.5);
        payload.put("comment", "FNT Order " + order.getOrderNumber());
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

    /** Shiprocket pickup nickname = seller business name (never ASVI / platform default). */
    private static String sellerBusinessPickupNickname(Seller seller) {
        if (seller == null || isBlank(seller.getBusinessName())) {
            throw new IllegalStateException(
                    "Seller business name is required for Shiprocket pickup. "
                            + "Update the seller profile business name first."
            );
        }
        String nickname = seller.getBusinessName().trim().replaceAll("\\s+", " ");
        // Shiprocket pickup nicknames are short; keep a safe length.
        if (nickname.length() > 36) {
            nickname = nickname.substring(0, 36).trim();
        }
        return nickname;
    }

    /**
     * Ensure seller business name exists as a Shiprocket pickup location,
     * creating it from the seller warehouse/business address when missing.
     */
    private void ensureSellerPickupRegistered(Seller seller, String pickupNickname) {
        String token = getToken();
        if (pickupExists(token, pickupNickname)) {
            log.info("Shiprocket pickup already exists nickname={}", pickupNickname);
            return;
        }

        String address = firstNonBlank(
                seller.getWarehouseAddress(),
                seller.getAddress()
        );
        if (isBlank(address)) {
            throw new IllegalStateException(
                    "Seller '" + pickupNickname
                            + "' has no warehouse/business address. "
                            + "Add warehouse address on the seller profile before Push to Shiprocket."
            );
        }

        String city = firstNonBlank(seller.getWarehouseCity(), seller.getCity());
        String state = firstNonBlank(seller.getWarehouseState(), seller.getState());
        String country = firstNonBlank(seller.getWarehouseCountry(), seller.getCountry(), "India");
        String pin = seller.getPincode() != null
                ? seller.getPincode().replaceAll("[^0-9]", "")
                : "";
        if (isBlank(city) || isBlank(state) || pin.length() != 6) {
            throw new IllegalStateException(
                    "Seller '" + pickupNickname
                            + "' needs city, state and 6-digit pincode for Shiprocket pickup. "
                            + "Update seller warehouse/business address."
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
        addPickup.put("address", address.trim());
        if (!isBlank(seller.getWarehouseArea()) || !isBlank(seller.getArea())) {
            addPickup.put("address_2", firstNonBlank(seller.getWarehouseArea(), seller.getArea()));
        }
        addPickup.put("city", city.trim());
        addPickup.put("state", state.trim());
        addPickup.put("country", country.trim());
        addPickup.put("pin_code", pin);

        log.info("Registering Shiprocket pickup nickname={} city={} pin={}",
                pickupNickname, city, pin);
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
            // Already exists is OK
            String lower = (apiBody != null ? apiBody : e.getMessage()).toLowerCase(Locale.ENGLISH);
            if (lower.contains("already") || lower.contains("exist")) {
                log.info("Shiprocket pickup already present nickname={}", pickupNickname);
                return;
            }
            throw new IllegalStateException(
                    "Could not register seller pickup '" + pickupNickname
                            + "' in Shiprocket: " + (isBlank(apiBody) ? e.getMessage() : apiBody),
                    e
            );
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
