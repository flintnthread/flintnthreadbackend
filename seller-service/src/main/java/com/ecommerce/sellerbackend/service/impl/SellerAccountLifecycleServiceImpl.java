package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.entity.OrderItem;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.ForbiddenException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.SellerAccountLifecycleService;
import com.ecommerce.sellerbackend.util.SellerAccountLifecycleSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerAccountLifecycleServiceImpl implements SellerAccountLifecycleService {

    private final SellerRepository sellerRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEligibility(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        return buildEligibility(seller);
    }

    @Override
    @Transactional
    public Map<String, Object> getStatus(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        maybeMarkExpired(seller);
        seller = requireSeller(sellerId);
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = SellerAccountLifecycleSupport.parseDateTime(lifecycle.get("expiresAt"));
        Long remainingSeconds = null;
        if (expiresAt != null && (seller.getStatus() == SellerAccountStatus.inactive
                || seller.getStatus() == SellerAccountStatus.act_req)) {
            remainingSeconds = Math.max(0, java.time.Duration.between(now, expiresAt).getSeconds());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", seller.getStatus() != null ? seller.getStatus().name() : null);
        out.put("displayStatus", displayStatus(seller.getStatus(), lifecycle));
        out.put("serverNow", SellerAccountLifecycleSupport.format(now));
        out.put("duration", SellerAccountLifecycleSupport.stringVal(lifecycle.get("duration")));
        out.put("requestedAt", SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestedAt")));
        out.put("startedAt", SellerAccountLifecycleSupport.stringVal(lifecycle.get("startedAt")));
        out.put("expiresAt", SellerAccountLifecycleSupport.stringVal(lifecycle.get("expiresAt")));
        out.put("requestType", SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestType")));
        out.put("requestStatus", SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestStatus")));
        out.put("remainingSeconds", remainingSeconds);
        out.put("canRequestDeactivation", seller.getStatus() == SellerAccountStatus.active);
        out.put("canRequestActivation", seller.getStatus() == SellerAccountStatus.inactive
                || (seller.getStatus() == SellerAccountStatus.act_req));
        out.put("deactivated", seller.getStatus() == SellerAccountStatus.inactive
                || seller.getStatus() == SellerAccountStatus.act_req);
        out.put("eligibility", buildEligibility(seller));
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> requestDeactivation(Long sellerId, String duration) {
        Seller seller = requireSeller(sellerId);
        if (seller.getStatus() != SellerAccountStatus.active) {
            throw new ForbiddenException(
                    "Only active seller accounts can request deactivation. Current status: "
                            + (seller.getStatus() != null ? seller.getStatus().name() : "unknown")
            );
        }

        Map<String, Object> eligibility = buildEligibility(seller);
        if (!Boolean.TRUE.equals(eligibility.get("eligible"))) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("success", false);
            blocked.put("eligible", false);
            blocked.put("message", "Account is not eligible for deactivation yet.");
            blocked.putAll(eligibility);
            return blocked;
        }

        String normalizedDuration = SellerAccountLifecycleSupport.normalizeDuration(duration);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("requestType", SellerAccountLifecycleSupport.TYPE_DEACTIVATION);
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_PENDING);
        lifecycle.put("duration", normalizedDuration);
        lifecycle.put("requestedAt", SellerAccountLifecycleSupport.format(now));
        lifecycle.put("startedAt", null);
        lifecycle.put("expiresAt", null);
        lifecycle.put("approvedBy", null);
        lifecycle.put("approvedAt", null);

        seller.setStatus(SellerAccountStatus.deact_req);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "Deactivation request submitted. Waiting for admin approval.");
        out.put("status", seller.getStatus().name());
        out.put("duration", normalizedDuration);
        out.put("requestedAt", SellerAccountLifecycleSupport.format(now));
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> requestActivation(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        maybeMarkExpired(seller);
        seller = requireSeller(sellerId);

        if (seller.getStatus() == SellerAccountStatus.act_req) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("alreadyRequested", true);
            out.put("message", "Activation request is already pending admin approval.");
            out.put("status", seller.getStatus().name());
            return out;
        }
        if (seller.getStatus() != SellerAccountStatus.inactive) {
            throw new ForbiddenException(
                    "Activation can only be requested while the account is deactivated."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        lifecycle.put("requestType", SellerAccountLifecycleSupport.TYPE_ACTIVATION);
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_PENDING);
        lifecycle.put("activationRequestedAt", SellerAccountLifecycleSupport.format(now));
        // Keep previous deactivation duration/expiry for admin context.
        seller.setStatus(SellerAccountStatus.act_req);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "Activation request submitted. Waiting for admin approval.");
        out.put("status", seller.getStatus().name());
        out.put("requestedAt", SellerAccountLifecycleSupport.format(now));
        return out;
    }

    private void maybeMarkExpired(Seller seller) {
        if (seller.getStatus() != SellerAccountStatus.inactive
                && seller.getStatus() != SellerAccountStatus.act_req) {
            return;
        }
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        LocalDateTime expiresAt = SellerAccountLifecycleSupport.parseDateTime(lifecycle.get("expiresAt"));
        if (expiresAt == null || !LocalDateTime.now().isAfter(expiresAt)) {
            return;
        }
        String reqStatus = SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestStatus"));
        if (SellerAccountLifecycleSupport.REQ_EXPIRED.equalsIgnoreCase(reqStatus)) {
            return;
        }
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_EXPIRED);
        lifecycle.put("expiredAt", SellerAccountLifecycleSupport.format(LocalDateTime.now()));
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);
    }

    private Map<String, Object> buildEligibility(Seller seller) {
        List<Map<String, Object>> pendingOrders = findBlockingOrders(seller.getId());
        List<Map<String, Object>> inStockProducts = findInStockProducts(seller.getId());

        boolean eligible = pendingOrders.isEmpty() && inStockProducts.isEmpty();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eligible", eligible);
        out.put("pendingOrdersCount", pendingOrders.size());
        out.put("activeShipmentsCount", pendingOrders.stream()
                .filter(o -> Boolean.TRUE.equals(o.get("hasActiveShipment")))
                .count());
        out.put("productsInStockCount", inStockProducts.size());
        out.put("pendingOrders", pendingOrders);
        out.put("inStockProducts", inStockProducts);
        out.put("allProductsOutOfStock", inStockProducts.isEmpty());
        if (!eligible) {
            if (!pendingOrders.isEmpty()) {
                out.put("blockReason", "PENDING_ORDERS");
                out.put("message", "You currently have pending orders that must be completed before your account can be deactivated.");
            } else {
                out.put("blockReason", "PRODUCTS_IN_STOCK");
                out.put("message", "Your account has products that are still in stock. Set them to Out of Stock first.");
            }
        } else {
            out.put("blockReason", null);
            out.put("message", "Your account is eligible for temporary deactivation.");
        }
        return out;
    }

    private List<Map<String, Object>> findBlockingOrders(Long sellerId) {
        List<Object[]> results = orderItemRepository.findOrderItemsWithOrderAndProductBySellerId(sellerId);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        Map<Long, List<OrderItem>> byOrder = new LinkedHashMap<>();
        Map<Long, Order> ordersById = new HashMap<>();
        Map<Long, Product> productsById = new HashMap<>();

        for (Object[] result : results) {
            OrderItem item = (OrderItem) result[0];
            Order order = (Order) result[1];
            Product product = (Product) result[2];

            if (item.getOrderId() != null) {
                byOrder.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
            if (order != null) {
                ordersById.put(order.getId(), order);
            }
            if (product != null && item.getProductId() != null) {
                productsById.put(item.getProductId(), product);
            }
        }

        List<Map<String, Object>> blocking = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : byOrder.entrySet()) {
            Order order = ordersById.get(entry.getKey());
            if (order == null) {
                continue;
            }
            String rawStatus = firstNonBlank(
                    entry.getValue().stream().map(OrderItem::getStatus).filter(Objects::nonNull).findFirst().orElse(null),
                    order.getOrderStatus()
            );
            String uiStatus = toUiStatus(rawStatus);
            if (!isBlockingUiStatus(uiStatus)) {
                continue;
            }
            boolean hasActiveShipment = hasActiveShipment(order);
            OrderItem first = entry.getValue().get(0);
            Product product = first.getProductId() != null ? productsById.get(first.getProductId()) : null;
            String productName = product != null && product.getName() != null ? product.getName() : "Product";
            int qty = entry.getValue().stream()
                    .mapToInt(i -> i.getQuantity() != null && i.getQuantity() > 0 ? i.getQuantity() : 1)
                    .sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", order.getId());
            row.put("orderNumber", order.getOrderNumber());
            row.put("productName", productName);
            row.put("quantity", qty);
            row.put("orderStatus", uiStatus);
            row.put("shipmentStatus", resolveShipmentStatus(order, hasActiveShipment));
            row.put("hasActiveShipment", hasActiveShipment);
            row.put("shiprocketAwb", order.getShiprocketAwbCode());
            blocking.add(row);
        }
        return blocking;
    }

    private List<Map<String, Object>> findInStockProducts(Long sellerId) {
        List<Object[]> results = productRepository.findInStockProductsWithStockCount(sellerId);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> inStock = new ArrayList<>();
        for (Object[] result : results) {
            Long productId = ((Number) result[0]).longValue();
            String productName = (String) result[1];
            Integer stock = ((Number) result[2]).intValue();

            if (stock > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", productId);
                row.put("name", productName != null ? productName : "Product");
                row.put("stock", stock);
                inStock.add(row);
            }
        }
        return inStock;
    }

    private static boolean hasActiveShipment(Order order) {
        if (order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank()) {
            String sr = order.getShiprocketStatus() != null
                    ? order.getShiprocketStatus().trim().toLowerCase(Locale.ENGLISH)
                    : "";
            if (sr.contains("deliver") || sr.contains("rto") || sr.contains("cancel")) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static String resolveShipmentStatus(Order order, boolean hasActiveShipment) {
        if (!hasActiveShipment) {
            return "Not Shipped";
        }
        if (order.getShiprocketAwbCode() != null && !order.getShiprocketAwbCode().isBlank()) {
            return order.getShiprocketStatus() != null && !order.getShiprocketStatus().isBlank()
                    ? order.getShiprocketStatus()
                    : "AWB Assigned";
        }
        return order.getShiprocketStatus() != null && !order.getShiprocketStatus().isBlank()
                ? order.getShiprocketStatus()
                : "Shipment Created";
    }

    private static boolean isBlockingUiStatus(String uiStatus) {
        return "Pending".equals(uiStatus)
                || "Processing".equals(uiStatus)
                || "Shipped".equals(uiStatus);
    }

    private static String toUiStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Pending";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "pending", "sent_to_seller", "awaiting_payment", "awaiting_processing",
                    "new", "placed" -> "Pending";
            case "confirmed", "processing", "packed", "awb_assigned", "pickup_scheduled",
                    "accepted", "awaiting_courier" -> "Processing";
            case "picked_up", "in_transit", "out_for_delivery", "shipped", "ready_to_ship" -> "Shipped";
            case "delivered", "completed" -> "Delivered";
            case "returned", "return", "refunded", "rto_initiated", "rto_delivered", "replacement" -> "Returned";
            case "cancelled", "canceled", "rejected" -> "Cancelled";
            default -> {
                String lower = raw.trim().toLowerCase(Locale.ROOT);
                if (lower.contains("deliver")) {
                    yield "Delivered";
                }
                if (lower.contains("cancel")) {
                    yield "Cancelled";
                }
                if (lower.contains("return") || lower.contains("rto")) {
                    yield "Returned";
                }
                if (lower.contains("ship") || lower.contains("transit") || lower.contains("pickup")) {
                    yield "Shipped";
                }
                yield "Processing";
            }
        };
    }

    private static String displayStatus(SellerAccountStatus status, Map<String, Object> lifecycle) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case active -> "Active";
            case deact_req -> "Deactivation Requested";
            case inactive -> {
                String req = SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestStatus"));
                if (SellerAccountLifecycleSupport.REQ_EXPIRED.equalsIgnoreCase(req)) {
                    yield "Deactivation Expired";
                }
                yield "Deactivated";
            }
            case act_req -> "Activation Requested";
            case suspended -> "Suspended";
            case rejected -> "Rejected";
            case pending -> "Pending";
            case email_pending -> "Email Pending";
        };
    }

    private Seller requireSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
