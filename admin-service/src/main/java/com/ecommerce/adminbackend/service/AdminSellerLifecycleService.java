package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.OrderItem;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerAccountStatus;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.util.SellerAccountLifecycleSupport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
public class AdminSellerLifecycleService {

    private static final Logger log = LogFactory.getLogger(AdminSellerLifecycleService.class);

    private final SellerRepository sellerRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> listRequests(String type) {
        String t = type == null ? "deactivation" : type.trim().toLowerCase(Locale.ENGLISH);
        List<Seller> sellers;
        if ("activation".equals(t)) {
            sellers = sellerRepository.findByStatusIn(List.of(SellerAccountStatus.act_req));
        } else {
            sellers = sellerRepository.findByStatusIn(List.of(SellerAccountStatus.deact_req));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Seller seller : sellers) {
            rows.add(toRequestSummary(seller));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", t);
        out.put("count", rows.size());
        out.put("items", rows);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRequestDetails(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        Map<String, Object> eligibility = buildEligibility(seller.getId());
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        Map<String, Object> out = toRequestSummary(seller);
        out.put("eligibility", eligibility);
        out.put("lifecycle", lifecycle);
        return out;
    }

    @Transactional
    public Map<String, Object> approveDeactivation(Long sellerId, Long adminId) {
        Seller seller = requireSeller(sellerId);
        if (seller.getStatus() != SellerAccountStatus.deact_req) {
            throw new IllegalStateException("Seller does not have a pending deactivation request.");
        }
        Map<String, Object> eligibility = buildEligibility(seller.getId());
        if (!Boolean.TRUE.equals(eligibility.get("eligible"))) {
            throw new IllegalStateException(
                    "Seller is no longer eligible for deactivation. "
                            + String.valueOf(eligibility.get("message"))
            );
        }

        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        String duration = SellerAccountLifecycleSupport.normalizeDuration(
                SellerAccountLifecycleSupport.stringVal(lifecycle.get("duration"))
        );
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = SellerAccountLifecycleSupport.computeExpiresAt(now, duration);

        lifecycle.put("requestType", SellerAccountLifecycleSupport.TYPE_DEACTIVATION);
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_APPROVED);
        lifecycle.put("duration", duration);
        lifecycle.put("startedAt", SellerAccountLifecycleSupport.format(now));
        lifecycle.put("expiresAt", SellerAccountLifecycleSupport.format(expiresAt));
        lifecycle.put("approvedBy", adminId);
        lifecycle.put("approvedAt", SellerAccountLifecycleSupport.format(now));

        seller.setStatus(SellerAccountStatus.inactive);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);
        log.info("Admin approved seller deactivation sellerId={} duration={} expiresAt={}",
                sellerId, duration, expiresAt);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("message", "Deactivation approved. Seller account is now deactivated.");
        out.put("status", seller.getStatus().name());
        out.put("startedAt", SellerAccountLifecycleSupport.format(now));
        out.put("expiresAt", SellerAccountLifecycleSupport.format(expiresAt));
        out.put("duration", duration);
        return out;
    }

    @Transactional
    public Map<String, Object> rejectDeactivation(Long sellerId, Long adminId, String reason) {
        Seller seller = requireSeller(sellerId);
        if (seller.getStatus() != SellerAccountStatus.deact_req) {
            throw new IllegalStateException("Seller does not have a pending deactivation request.");
        }
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        LocalDateTime now = LocalDateTime.now();
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_REJECTED);
        lifecycle.put("rejectedBy", adminId);
        lifecycle.put("rejectedAt", SellerAccountLifecycleSupport.format(now));
        lifecycle.put("rejectReason", reason);

        seller.setStatus(SellerAccountStatus.active);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);

        return Map.of(
                "success", true,
                "message", "Deactivation request rejected. Seller remains active.",
                "status", seller.getStatus().name()
        );
    }

    @Transactional
    public Map<String, Object> approveActivation(Long sellerId, Long adminId) {
        Seller seller = requireSeller(sellerId);
        if (seller.getStatus() != SellerAccountStatus.act_req
                && seller.getStatus() != SellerAccountStatus.inactive) {
            throw new IllegalStateException("Seller does not have a pending activation request.");
        }
        if (seller.getStatus() == SellerAccountStatus.inactive) {
            Map<String, Object> lifecycleCheck = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
            String reqType = SellerAccountLifecycleSupport.stringVal(lifecycleCheck.get("requestType"));
            String reqStatus = SellerAccountLifecycleSupport.stringVal(lifecycleCheck.get("requestStatus"));
            if (!SellerAccountLifecycleSupport.TYPE_ACTIVATION.equalsIgnoreCase(reqType)
                    || !SellerAccountLifecycleSupport.REQ_PENDING.equalsIgnoreCase(reqStatus)) {
                throw new IllegalStateException("Seller does not have a pending activation request.");
            }
        }

        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        LocalDateTime now = LocalDateTime.now();
        lifecycle.put("requestType", SellerAccountLifecycleSupport.TYPE_ACTIVATION);
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_APPROVED);
        lifecycle.put("activationApprovedBy", adminId);
        lifecycle.put("activationApprovedAt", SellerAccountLifecycleSupport.format(now));
        // Clear schedule so countdown stops; do NOT auto-restock products.
        lifecycle.put("startedAt", null);
        lifecycle.put("expiresAt", null);

        seller.setStatus(SellerAccountStatus.active);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);
        log.info("Admin approved seller activation sellerId={}", sellerId);

        return Map.of(
                "success", true,
                "message", "Activation approved. Seller is active again. Products remain Out of Stock until seller updates inventory.",
                "status", seller.getStatus().name()
        );
    }

    @Transactional
    public Map<String, Object> rejectActivation(Long sellerId, Long adminId, String reason) {
        Seller seller = requireSeller(sellerId);
        if (seller.getStatus() != SellerAccountStatus.act_req) {
            throw new IllegalStateException("Seller does not have a pending activation request.");
        }
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        LocalDateTime now = LocalDateTime.now();
        lifecycle.put("requestStatus", SellerAccountLifecycleSupport.REQ_REJECTED);
        lifecycle.put("activationRejectedBy", adminId);
        lifecycle.put("activationRejectedAt", SellerAccountLifecycleSupport.format(now));
        lifecycle.put("activationRejectReason", reason);

        seller.setStatus(SellerAccountStatus.inactive);
        seller.setAdminRemarks(SellerAccountLifecycleSupport.writeLifecycle(seller.getAdminRemarks(), lifecycle));
        sellerRepository.save(seller);

        return Map.of(
                "success", true,
                "message", "Activation request rejected. Seller remains deactivated.",
                "status", seller.getStatus().name()
        );
    }

    private Map<String, Object> toRequestSummary(Seller seller) {
        Map<String, Object> lifecycle = SellerAccountLifecycleSupport.readLifecycle(seller.getAdminRemarks());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sellerId", seller.getId());
        row.put("sellerCode", seller.getSellerUniqueId() != null ? seller.getSellerUniqueId() : ("SEL-" + seller.getId()));
        row.put("businessName", seller.getBusinessName());
        row.put("email", seller.getEmail());
        row.put("status", seller.getStatus() != null ? seller.getStatus().name() : null);
        row.put("requestType", SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestType")));
        row.put("duration", SellerAccountLifecycleSupport.stringVal(lifecycle.get("duration")));
        row.put("requestedAt", firstNonNull(
                SellerAccountLifecycleSupport.stringVal(lifecycle.get("activationRequestedAt")),
                SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestedAt"))
        ));
        row.put("startedAt", SellerAccountLifecycleSupport.stringVal(lifecycle.get("startedAt")));
        row.put("expiresAt", SellerAccountLifecycleSupport.stringVal(lifecycle.get("expiresAt")));
        row.put("requestStatus", SellerAccountLifecycleSupport.stringVal(lifecycle.get("requestStatus")));
        return row;
    }

    private Map<String, Object> buildEligibility(Long sellerId) {
        List<Map<String, Object>> pendingOrders = findBlockingOrders(sellerId);
        List<Map<String, Object>> inStock = findInStockProducts(sellerId);
        boolean eligible = pendingOrders.isEmpty() && inStock.isEmpty();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eligible", eligible);
        out.put("pendingOrdersCount", pendingOrders.size());
        out.put("activeShipmentsCount", pendingOrders.stream()
                .filter(o -> Boolean.TRUE.equals(o.get("hasActiveShipment")))
                .count());
        out.put("productsInStockCount", inStock.size());
        out.put("allProductsOutOfStock", inStock.isEmpty());
        out.put("pendingOrders", pendingOrders);
        out.put("inStockProducts", inStock);
        out.put("message", eligible
                ? "Eligible for deactivation."
                : (!pendingOrders.isEmpty()
                ? "Pending fulfillment orders block deactivation."
                : "Products still in stock block deactivation."));
        return out;
    }

    private List<Map<String, Object>> findBlockingOrders(Long sellerId) {
        List<Object[]> results = orderItemRepository.findOrderItemsWithOrderBySellerId(sellerId);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        Map<Long, List<OrderItem>> byOrder = new LinkedHashMap<>();
        Map<Long, Order> ordersById = new HashMap<>();

        for (Object[] result : results) {
            OrderItem item = (OrderItem) result[0];
            Order order = (Order) result[1];

            if (item.getOrderId() != null) {
                byOrder.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
            if (order != null) {
                ordersById.put(order.getId(), order);
            }
        }

        List<Map<String, Object>> blocking = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : byOrder.entrySet()) {
            Order order = ordersById.get(entry.getKey());
            if (order == null) {
                continue;
            }
            String raw = firstNonBlank(
                    entry.getValue().stream().map(OrderItem::getStatus).filter(Objects::nonNull).findFirst().orElse(null),
                    order.getOrderStatus()
            );
            String ui = toUiStatus(raw);
            if (!isBlocking(ui)) {
                continue;
            }
            boolean hasShipment = hasActiveShipment(order);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", order.getId());
            row.put("orderNumber", order.getOrderNumber());
            row.put("orderStatus", ui);
            row.put("shipmentStatus", hasShipment
                    ? (order.getShiprocketStatus() != null ? order.getShiprocketStatus() : "Shipment Created")
                    : "Not Shipped");
            row.put("hasActiveShipment", hasShipment);
            blocking.add(row);
        }
        return blocking;
    }

    private List<Map<String, Object>> findInStockProducts(Long sellerId) {
        List<Object[]> results = productRepository.findInStockProductsWithStockCount(sellerId);
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] result : results) {
            Long productId = ((Number) result[0]).longValue();
            String productName = (String) result[1];
            Integer stock = ((Number) result[2]).intValue();

            if (stock > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", productId);
                row.put("name", productName != null ? productName : "Product");
                row.put("stock", stock);
                rows.add(row);
            }
        }
        return rows;
    }

    private static boolean hasActiveShipment(Order order) {
        if (order.getShiprocketOrderId() == null || order.getShiprocketOrderId().isBlank()) {
            return false;
        }
        String sr = order.getShiprocketStatus() != null
                ? order.getShiprocketStatus().toLowerCase(Locale.ENGLISH) : "";
        return !(sr.contains("deliver") || sr.contains("rto") || sr.contains("cancel"));
    }

    private static boolean isBlocking(String ui) {
        return "Pending".equals(ui) || "Processing".equals(ui) || "Shipped".equals(ui);
    }

    private static String toUiStatus(String raw) {
        if (raw == null || raw.isBlank()) return "Pending";
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "pending", "sent_to_seller", "awaiting_payment", "awaiting_processing", "new", "placed" -> "Pending";
            case "confirmed", "processing", "packed", "awb_assigned", "pickup_scheduled", "accepted", "awaiting_courier" -> "Processing";
            case "picked_up", "in_transit", "out_for_delivery", "shipped", "ready_to_ship" -> "Shipped";
            case "delivered", "completed" -> "Delivered";
            case "returned", "return", "refunded", "rto_initiated", "rto_delivered", "replacement" -> "Returned";
            case "cancelled", "canceled", "rejected" -> "Cancelled";
            default -> "Processing";
        };
    }

    private Seller requireSeller(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
