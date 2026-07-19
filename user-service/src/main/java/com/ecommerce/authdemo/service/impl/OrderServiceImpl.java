package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.dto.Enum.AdminStatus;
import com.ecommerce.authdemo.dto.Enum.OrderStatus;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.event.OrderPlacedEvent;
import com.ecommerce.authdemo.service.*;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.repository.*;
import com.ecommerce.authdemo.util.SecurityUtil;
import com.ecommerce.authdemo.mail.OrderConfirmationEmailModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** Flat platform fee (INR) added to every placed order total. */
    private static final BigDecimal PLATFORM_FEE = BigDecimal.valueOf(9);

    private static final ZoneId ORDER_DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ORDER_CREATED_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy, h:mm a", Locale.ENGLISH);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final SecurityUtil securityUtil;
    private final ReferralService referralService;
    private final UserRepository userRepository;
    private final ShiprocketService shiprocketService;
    private final RazorpayService razorpayService;
    private final PushNotificationService pushNotificationService;
    private final ReferralTransactionRepository transactionRepository;

    private final EmailService emailService;  // your existing service
    private final SmsService smsService;      // your SMS/WhatsApp service
    private final ApplicationEventPublisher applicationEventPublisher;
    private final WalletService walletService;
    private final OrderItemCustomDetailService orderItemCustomDetailService;
    private final SellerRepository sellerRepository;
    private final AdminUserRepository adminUserRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.public-web-base-url:https://flintnthread.in}")
    private String publicWebBaseUrl;

    @Value("${app.seller.frontend.base-url:https://flintnthread.in}")
    private String sellerFrontendBaseUrl;

    @Value("${app.admin.frontend-url:https://flintnthread.in}")
    private String adminFrontendBaseUrl;

    @Value("${app.mail.admin-notify:admin@flintnthread.in}")
    private String adminNotifyEmail;


    @Override
    @Transactional
    public OrderResponseDTO placeOrder(PlaceOrderRequestDTO dto) {

        try {
            validatePlaceOrderRequest(dto);

            Long userId = securityUtil.getCurrentUserId();

            User user =
                    userRepository
                            .findById(userId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"
                                    ));
            CartResponseDTO cart = dto.getAddressId() != null
                    ? cartService.getCart(Math.toIntExact(dto.getAddressId()))
                    : cartService.getCart();

            List<CartItemResponseDTO> checkoutItems =
                    resolveCheckoutCartItems(cart.getItems(), dto.getItemIds());

            if (checkoutItems.isEmpty()) {
                throw new OrderException("Cart is empty");
            }

            boolean partialCheckout =
                    dto.getItemIds() != null && !dto.getItemIds().isEmpty();

            // Customization details are collected after order placement (order success / order details).

            // ✅ FINAL STOCK VALIDATION + DECREMENT (atomic)
            // If any item cannot be decremented (insufficient stock), we fail and the whole transaction rolls back.
            for (CartItemResponseDTO item : checkoutItems) {
                int requestedQty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (requestedQty <= 0) continue;
                
                int updated = productVariantRepository.decrementStockIfAvailable(
                        item.getVariantId(),
                        requestedQty
                );
                if (updated == 0) {
                    Integer current = productVariantRepository.findStockByVariantId(item.getVariantId()).orElse(0);
                    String productName = item.getProductName() != null ? item.getProductName() : "Product";
                    String color = item.getColorName() != null ? item.getColorName() : "";
                    
                    String productDesc = productName;
                    if (!color.isEmpty()) {
                        productDesc += " (" + color + ")";
                    }
                    
                    throw new OrderException("Only " + current + " " + productDesc + " available. You requested " + requestedQty + ". Please reduce quantity or try again later.");
                }
            }

            Address address = addressRepository
                    .findByIdAndUserId(Math.toIntExact(dto.getAddressId()), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found for current user"));

            BigDecimal subtotal;
            BigDecimal shipping;
            BigDecimal discount;
            BigDecimal finalAmount;

            if (partialCheckout) {
                subtotal = sumCheckoutSubtotal(checkoutItems);
                discount = sumCheckoutDiscount(checkoutItems);
                shipping = cart.getPriceSummary() != null
                        && cart.getPriceSummary().getDeliveryCharge() != null
                        ? cart.getPriceSummary().getDeliveryCharge()
                        : BigDecimal.ZERO;
                finalAmount = subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
            } else {
                subtotal = cart.getPriceSummary().getSubtotal();
                shipping = cart.getPriceSummary().getDeliveryCharge();
                discount = cart.getPriceSummary().getDiscount();
                finalAmount = cart.getPriceSummary().getFinalTotal();
            }

            // Keep cart deliveryCharge in shippingAmount / finalAmount (do not zero shipping).

            // ✅ Apply inviter referral reward (10% off subtotal) when unlocked — not on referee signup
            double referralDiscountPercent = 0.0d;

            try {
                long paidOrders = orderRepository.countByUserIdAndPaymentStatus(userId, "paid");
                boolean noReferralPricingOrderYet =
                        !orderRepository.existsByUserIdAndReferralInviterDiscountAppliedTrue(userId);

                if (noReferralPricingOrderYet) {
                    BigDecimal discountPercent = referralService
                            .getAvailableReferralDiscountPercentForUser(userId);

                    referralDiscountPercent = (discountPercent != null)
                            ? discountPercent.doubleValue()
                            : 0.0d;
                }

            } catch (Exception e) {
                log.warn("[ORDER] referral discount lookup failed userId={}: {}", userId, e.getMessage());
            }

            boolean inviterReferralApplied = referralDiscountPercent > 0;

            if (referralDiscountPercent > 0) {
                BigDecimal extraDiscount = subtotal
                        .multiply(BigDecimal.valueOf(referralDiscountPercent))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                discount = discount.add(extraDiscount);
                finalAmount = subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
            }

            // Platform fee is always added to the payable order amount at checkout.
            finalAmount = finalAmount.add(PLATFORM_FEE).max(BigDecimal.ZERO);

            BigDecimal walletApplied = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(dto.getUseWallet())
                    && dto.getWalletAmount() != null
                    && dto.getWalletAmount() > 0) {
                BigDecimal maxWalletAllowed = finalAmount.setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal requestedWallet = BigDecimal.valueOf(dto.getWalletAmount())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                if (requestedWallet.compareTo(maxWalletAllowed) > 0) {
                    throw new OrderException(
                            "FNT Wallet amount exceeds order payable (max "
                                    + maxWalletAllowed + " INR)"
                    );
                }
                walletApplied = requestedWallet.min(finalAmount);
                if (walletApplied.compareTo(BigDecimal.ZERO) > 0) {
                    WalletResponse wallet =
                            walletService.getOrCreateWallet(Math.toIntExact(userId));
                    if (wallet.getBalance() == null
                            || wallet.getBalance().compareTo(walletApplied) < 0) {
                        throw new OrderException("Insufficient FNT wallet balance");
                    }
                    finalAmount = finalAmount.subtract(walletApplied).max(BigDecimal.ZERO);
                } else {
                    walletApplied = BigDecimal.ZERO;
                }
            }

            Order order = createOrder(
                    userId, dto, address, subtotal, shipping, discount, finalAmount,
                    walletApplied, inviterReferralApplied
            );
            order = orderRepository.save(order);

            if (walletApplied.compareTo(BigDecimal.ZERO) > 0) {
                walletService.debitForOrderPayment(
                        Math.toIntExact(userId),
                        order.getId(),
                        walletApplied,
                        order.getOrderNumber()
                );
            }

            if ("paid".equalsIgnoreCase(order.getPaymentStatus())) {
                processReferralAfterOrderPaid(order);
            }

            boolean pendingOnline = isPendingOnlinePayment(order);
            if (!pendingOnline) {
                pushNotificationService.notifyUser(
                        userId,
                        "Order #" + order.getOrderNumber() + " placed",
                        "Your order has been placed successfully.",
                        "order",
                        "/orders?orderId=" + order.getId()
                );
            } else {
                pushNotificationService.notifyUser(
                        userId,
                        "Complete payment for #" + order.getOrderNumber(),
                        "Your order is waiting for payment. Complete Razorpay to confirm.",
                        "order",
                        "/orders?orderId=" + order.getId()
                );
            }

            List<OrderItemDTO> itemDTOList = createOrderItems(order, checkoutItems);

            if (!pendingOnline) {
                sendOrderConfirmationEmailSafely(
                        user,
                        order,
                        itemDTOList,
                        subtotal,
                        shipping,
                        discount,
                        walletApplied
                );
            }

            applicationEventPublisher.publishEvent(
                    new OrderPlacedEvent(Math.toIntExact(order.getId()))
            );

            // For unpaid online checkout, keep cart until Razorpay verify succeeds.
            if (!pendingOnline) {
                clearCartSafely(userId);
            }

            // Shiprocket after commit so COD/place API returns immediately (no 15s client timeout).
            scheduleShiprocketAfterCommit(order);

            return buildOrderResponse(order, itemDTOList);

        } catch (OptimisticLockException e) {
            throw new OrderException("Stock updated by another user. Please try again.");
        }

    }

    /**
     * Normal shopper order history is scoped to the authenticated user only.
     * Linked-phone sibling merge is not applied here (avoids mixed profiles on Switch Account).
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(String status) {
        User current = securityUtil.getCurrentUser();
        if (current == null || current.getId() == null) {
            throw new OrderException("User not authenticated");
        }

        List<Order> orders = loadOrdersForUserList(current.getId(), status);

        return orders.stream()
                .map(order -> {
                    try {
                        return buildOrderSummaryResponseLite(order);
                    } catch (Exception e) {
                        log.warn(
                                "Skipping order id={} in list: {}",
                                order.getId(),
                                e.getMessage()
                        );
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private record LinkedPhoneScope(Set<Long> userIds, Set<String> phoneVariants) {}

    /**
     * Users who share the logged-in account's registered contact number (Switch Account),
     * plus normalized phone variants for shipping_phone matching.
     */
    private LinkedPhoneScope resolveLinkedPhoneScope(User current) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (current.getId() != null) {
            userIds.add(current.getId());
        }

        Set<String> phoneVariants = phoneLookupVariants(current.getContactNumber());
        if (!phoneVariants.isEmpty()) {
            for (User sibling : userRepository.findAllByContactNumberIn(phoneVariants)) {
                if (sibling.getId() != null) {
                    userIds.add(sibling.getId());
                }
                phoneVariants.addAll(phoneLookupVariants(sibling.getContactNumber()));
            }
        }

        if (userIds.isEmpty()) {
            throw new OrderException("User not authenticated");
        }
        return new LinkedPhoneScope(userIds, phoneVariants);
    }

    private static Set<String> phoneLookupVariants(String phone) {
        Set<String> variants = new LinkedHashSet<>();
        if (phone == null || phone.isBlank()) {
            return variants;
        }
        String trimmed = phone.trim();
        variants.add(trimmed);
        String digits = trimmed.replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            variants.add(digits);
        }
        if (digits.length() >= 10) {
            String last10 = digits.substring(digits.length() - 10);
            variants.add(last10);
            variants.add("91" + last10);
            variants.add("+91" + last10);
            variants.add("0" + last10);
        }
        return variants;
    }

    private boolean canAccessOrder(Order order, LinkedPhoneScope scope) {
        if (order == null || scope == null) {
            return false;
        }
        if (order.getUserId() != null && scope.userIds().contains(order.getUserId())) {
            return true;
        }
        if (order.getShippingPhone() != null && !scope.phoneVariants().isEmpty()) {
            Set<String> shippingVariants = phoneLookupVariants(order.getShippingPhone());
            for (String v : shippingVariants) {
                if (scope.phoneVariants().contains(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void assertCanAccessOrder(Order order) {
        User current = securityUtil.getCurrentUser();
        LinkedPhoneScope scope = resolveLinkedPhoneScope(current);
        if (!canAccessOrder(order, scope)) {
            throw new OrderException("Access denied");
        }
    }

    private List<Order> loadOrdersForLinkedPhoneScope(LinkedPhoneScope scope, String status) {
        try {
            if (scope.phoneVariants().isEmpty()) {
                if (status == null || status.equalsIgnoreCase("all")) {
                    return orderRepository.findByUserIdInOrderByCreatedAtDesc(scope.userIds());
                }
                return orderRepository.findByUserIdInAndOrderStatusOrderByCreatedAtDesc(
                        scope.userIds(), status);
            }
            if (status == null || status.equalsIgnoreCase("all")) {
                return orderRepository.findVisibleForLinkedPhoneAccounts(
                        scope.userIds(), scope.phoneVariants());
            }
            return orderRepository.findVisibleForLinkedPhoneAccountsAndStatus(
                    scope.userIds(), scope.phoneVariants(), status);
        } catch (Exception jpaError) {
            log.warn(
                    "JPA linked-phone order list failed for userIds={}, status={}: {} — falling back",
                    scope.userIds(),
                    status,
                    jpaError.getMessage()
            );
            Long primaryId = scope.userIds().iterator().next();
            return loadOrdersForUserList(primaryId, status);
        }
    }

    private List<Order> loadOrdersForUserList(Long userId, String status) {
        try {
            if (status == null || status.equalsIgnoreCase("all")) {
                return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            }
            return orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, status);
        } catch (Exception jpaError) {
            log.warn(
                    "JPA order list failed for userId={}, status={}: {} — using native fallback",
                    userId,
                    status,
                    jpaError.getMessage()
            );
            List<Object[]> rows =
                    status == null || status.equalsIgnoreCase("all")
                            ? orderRepository.findSummaryRowsByUserId(userId)
                            : orderRepository.findSummaryRowsByUserIdAndStatus(userId, status);
            return rows.stream()
                    .map(this::mapNativeOrderSummaryRow)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private Order mapNativeOrderSummaryRow(Object[] row) {
        if (row == null || row.length < 10) {
            return null;
        }
        Order order = new Order();
        order.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
        order.setUserId(row[1] != null ? ((Number) row[1]).longValue() : null);
        order.setOrderNumber(row[2] != null ? String.valueOf(row[2]) : null);
        order.setTotalAmount(row[3] != null ? ((Number) row[3]).doubleValue() : null);
        order.setShippingAmount(row[4] != null ? ((Number) row[4]).doubleValue() : null);
        order.setDiscountAmount(row[5] != null ? ((Number) row[5]).doubleValue() : null);
        order.setPaymentMethod(row[6] != null ? String.valueOf(row[6]) : null);
        order.setPaymentStatus(row[7] != null ? String.valueOf(row[7]) : null);
        order.setOrderStatus(row[8] != null ? String.valueOf(row[8]) : null);
        if (row[9] instanceof java.time.LocalDateTime ldt) {
            order.setCreatedAt(ldt);
        } else if (row[9] instanceof java.sql.Timestamp ts) {
            order.setCreatedAt(ts.toLocalDateTime());
        }
        order.setRazorpayPaymentId(row.length > 10 && row[10] != null ? String.valueOf(row[10]) : null);
        if (row.length > 11) {
            order.setShiprocketAwbCode(row[11] != null ? String.valueOf(row[11]) : null);
        }
        if (row.length > 12) {
            order.setShiprocketCourierName(row[12] != null ? String.valueOf(row[12]) : null);
        }
        if (row.length > 13) {
            order.setShiprocketTrackingUrl(row[13] != null ? String.valueOf(row[13]) : null);
        }
        if (row.length > 14) {
            order.setShiprocketStatus(row[14] != null ? String.valueOf(row[14]) : null);
        }
        return order.getId() != null ? order : null;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        assertCanAccessOrder(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItemDTO> itemDTOList = items.stream().map(this::buildOrderItemDTO).toList();

        return buildDetailedOrderResponse(order, itemDTOList);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getPublicOrderDetails(
            Long orderId,
            String orderNumber,
            Long productId,
            Integer lineIndex,
            Long sellerId,
            String productIdsRaw
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!matchesOrderNumber(order, orderNumber)) {
            throw new OrderException("Invalid order link");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItemDTO> itemDTOList = items.stream().map(this::buildOrderItemDTO).toList();
        List<Long> productIds = parseProductIds(productIdsRaw);
        boolean scoped = (productId != null && productId > 0)
                || (lineIndex != null && lineIndex > 0)
                || (sellerId != null && sellerId > 0)
                || !productIds.isEmpty();
        List<OrderItemDTO> visibleItems = scoped
                ? filterPublicOrderItems(itemDTOList, productId, lineIndex, sellerId, productIds)
                : itemDTOList;
        if (scoped && visibleItems.isEmpty()) {
            throw new OrderException("Product from this invoice could not be found");
        }
        return buildDetailedOrderResponse(order, visibleItems);
    }

    private List<Long> parseProductIds(String productIdsRaw) {
        if (productIdsRaw == null || productIdsRaw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(productIdsRaw.split("[,|]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<OrderItemDTO> filterPublicOrderItems(
            List<OrderItemDTO> items,
            Long productId,
            Integer lineIndex,
            Long sellerId,
            List<Long> productIds
    ) {
        if (productIds != null && !productIds.isEmpty()) {
            java.util.Set<Long> idSet = new java.util.HashSet<>(productIds);
            List<OrderItemDTO> matched = items.stream()
                    .filter(item -> item.getProductId() != null && idSet.contains(item.getProductId()))
                    .toList();
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        if (lineIndex != null && lineIndex > 0 && lineIndex <= items.size()) {
            return List.of(items.get(lineIndex - 1));
        }
        if (productId != null && productId > 0) {
            List<OrderItemDTO> matched = items.stream()
                    .filter(item -> productId.equals(item.getProductId()))
                    .toList();
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        if (sellerId != null && sellerId > 0) {
            List<OrderItemDTO> matched = items.stream()
                    .filter(item -> sellerId.equals(item.getSellerId()))
                    .toList();
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        return List.of();
    }

    private boolean matchesOrderNumber(Order order, String provided) {
        String expected = normalizeOrderNumber(order.getOrderNumber());
        String actual = normalizeOrderNumber(provided);
        return expected != null && actual != null && expected.equals(actual);
    }

    private String normalizeOrderNumber(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceFirst("^#", "").toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public CancelOrderResponseDTO cancelOrder(
            Long orderId,
            String cancelReason,
            boolean refundToWallet
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        assertCanAccessOrder(order);

        String status =
                order.getOrderStatus() != null
                        ? order.getOrderStatus().toLowerCase()
                        : "";

        log.info(
                "Cancel order request orderId={} currentStatus={}",
                orderId,
                status
        );

        if ("cancelled".equals(status)) {
            log.info("Order already cancelled orderId={}", orderId);
            if (refundToWallet && shouldCreditWalletOnCancel(order)) {
                BigDecimal walletCreditAmount = resolveOrderRefundAmount(order, true);
                boolean walletCredited = false;
                if (walletCreditAmount.compareTo(BigDecimal.ZERO) > 0) {
                    walletCredited = walletService.creditOrderCancellationRefund(
                            Math.toIntExact(order.getUserId()),
                            order.getId(),
                            walletCreditAmount,
                            order.getOrderNumber()
                    );
                    if (!walletCredited) {
                        walletCredited = walletService
                                .findOrderCancellationRefundAmount(
                                        Math.toIntExact(order.getUserId()),
                                        order.getId()
                                )
                                .isPresent();
                    }
                }
                String message = walletCredited
                        ? "Order already cancelled. "
                                + walletCreditAmount
                                + " has been added to your FNT Wallet."
                        : "Order already cancelled";
                return CancelOrderResponseDTO.builder()
                        .walletCredited(walletCredited)
                        .walletCreditAmount(
                                walletCredited ? walletCreditAmount : BigDecimal.ZERO
                        )
                        .message(message)
                        .build();
            }
            return CancelOrderResponseDTO.builder()
                    .walletCredited(false)
                    .walletCreditAmount(BigDecimal.ZERO)
                    .message("Order already cancelled")
                    .build();
        }

        List<String> blockedStatuses = Arrays.asList(
                "delivered",
                "out_for_delivery",
                "picked_up",
                "in_transit",
                "rto_delivered"
        );

        if (blockedStatuses.contains(status)) {
            throw new OrderException("Order cannot be cancelled now");
        }

        List<OrderItem> items =
                orderItemRepository.findByOrderId(orderId);

        for (OrderItem item : items) {
            try {
                ProductVariant variant =
                        productVariantRepository.findById(
                                item.getVariantId()
                        ).orElseThrow(() ->
                                new RuntimeException("Variant not found"));

                int currentStock =
                        variant.getStock() != null
                                ? variant.getStock()
                                : 0;

                variant.setStock(currentStock + item.getQuantity());
                productVariantRepository.save(variant);

                log.info(
                        "Stock restored variantId={} restoredQty={}",
                        variant.getId(),
                        item.getQuantity()
                );

            } catch (Exception e) {
                log.error(
                        "Stock restore failed for variant={} reason={}",
                        item.getVariantId(),
                        e.getMessage(),
                        e
                );
            }
        }

        boolean shiprocketCancelled = false;
        boolean shiprocketCancelAttempted = false;

        try {
            String shiprocketOrderId = order.getShiprocketOrderId();

            log.info(
                    "SHIPROCKET CANCEL START orderNumber={} orderId={}",
                    order.getOrderNumber(),
                    shiprocketOrderId
            );

            if (shiprocketOrderId != null && !shiprocketOrderId.isBlank()) {
                shiprocketCancelAttempted = true;
                shiprocketCancelled =
                        shiprocketService.cancelShipment(shiprocketOrderId);

                log.info(
                        "SHIPROCKET CANCEL RESULT orderNumber={} success={}",
                        order.getOrderNumber(),
                        shiprocketCancelled
                );
            } else {
                // No remote Shiprocket order to cancel — local cancel is enough.
                shiprocketCancelled = true;
                log.warn(
                        "Shiprocket order ID missing for order={}, cancelling locally",
                        order.getOrderNumber()
                );
            }
        } catch (Exception e) {
            log.error(
                    "Shiprocket cancellation failed order={} reason={}",
                    order.getOrderNumber(),
                    e.getMessage(),
                    e
            );
        }

        // Always cancel locally once past blocked statuses. Shiprocket cancel is
        // best-effort so a courier API failure does not block the shopper.
        // Keep order_items + status history in sync so seller/admin panels see Cancelled
        // (seller UI prefers line-item status over orders.order_status).
        for (OrderItem item : items) {
            item.setStatus("cancelled");
        }
        if (!items.isEmpty()) {
            orderItemRepository.saveAll(items);
        }

        order.setOrderStatus("cancelled");
        if (shiprocketCancelled || !shiprocketCancelAttempted) {
            order.setShiprocketStatus("cancelled");
            log.info(
                    "Order fully cancelled orderNumber={} shiprocketOrderId={}",
                    order.getOrderNumber(),
                    order.getShiprocketOrderId()
            );
        } else {
            order.setShiprocketStatus("cancel_failed");
            log.warn(
                    "Order cancelled locally but Shiprocket cancel failed orderNumber={} shiprocketOrderId={}",
                    order.getOrderNumber(),
                    order.getShiprocketOrderId()
            );
        }

        order.setCancelReason(cancelReason);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        try {
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.CANCELLED)
                    .comment(
                            cancelReason != null && !cancelReason.isBlank()
                                    ? cancelReason.trim()
                                    : "Cancelled by customer"
                    )
                    .createdBy(order.getUserId())
                    .build();
            orderStatusHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn(
                    "Skipping order_status_history insert for cancelled orderId={}",
                    orderId,
                    e
            );
        }

        boolean walletCredited = false;
        BigDecimal walletCreditAmount = BigDecimal.ZERO;
        String message = "Order cancelled successfully";

        if (shouldCreditWalletOnCancel(order)) {
            walletCreditAmount = resolveOrderRefundAmount(order, refundToWallet);
            if (walletCreditAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletCredited = walletService.creditOrderCancellationRefund(
                        Math.toIntExact(order.getUserId()),
                        order.getId(),
                        walletCreditAmount,
                        order.getOrderNumber()
                );
                if (walletCredited) {
                    message = "Order cancelled. "
                            + walletCreditAmount
                            + " has been added to your FNT Wallet.";
                }
            }
        }

        notifyOrderCancelledSafely(order, walletCredited, walletCreditAmount);

        return CancelOrderResponseDTO.builder()
                .walletCredited(walletCredited)
                .walletCreditAmount(walletCreditAmount)
                .message(message)
                .build();
    }

    /** Best-effort cancel push + email; failures must not roll back cancel. */
    private void notifyOrderCancelledSafely(
            Order order,
            boolean walletCredited,
            BigDecimal walletCreditAmount
    ) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        String orderLabel = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                ? order.getOrderNumber()
                : String.valueOf(order.getId());
        String refundNote = walletCredited && walletCreditAmount != null
                && walletCreditAmount.compareTo(BigDecimal.ZERO) > 0
                ? " Refund of ₹" + walletCreditAmount.setScale(2, java.math.RoundingMode.HALF_UP)
                        + " has been credited to your FNT Wallet."
                : "";

        try {
            pushNotificationService.notifyUser(
                    order.getUserId(),
                    "Order #" + orderLabel + " cancelled",
                    "Your order has been cancelled." + refundNote,
                    "order",
                    "/orders?orderId=" + order.getId()
            );
        } catch (Exception e) {
            log.warn(
                    "[NOTIFY] Cancel push failed order={}: {}",
                    orderLabel,
                    e.getMessage()
            );
        }

        try {
            User user = userRepository.findById(order.getUserId()).orElse(null);
            String recipient = firstNonBlank(
                    user != null ? user.getEmail() : null,
                    order.getShippingEmail()
            );
            if (recipient == null) {
                log.warn("[EMAIL] No recipient for cancel notice order={}", orderLabel);
                return;
            }
            String html = "<p>Hello,</p>"
                    + "<p>Your order <strong>#"
                    + escapeHtml(orderLabel)
                    + "</strong> (ID: "
                    + order.getId()
                    + ") has been cancelled.</p>"
                    + (refundNote.isBlank()
                            ? ""
                            : "<p>" + escapeHtml(refundNote.trim()) + "</p>")
                    + "<p>— Flint &amp; Thread</p>";
            emailService.sendNoticeEmail(
                    recipient,
                    "Order #" + orderLabel + " cancelled",
                    html
            );
        } catch (Exception e) {
            log.warn(
                    "[EMAIL] Cancel notice failed order={}: {}",
                    orderLabel,
                    e.getMessage()
            );
        }
    }

    private boolean shouldCreditWalletOnCancel(Order order) {
        if (isCodPaymentMethod(order.getPaymentMethod())) {
            return false;
        }
        String paymentStatus = order.getPaymentStatus() != null
                ? order.getPaymentStatus().trim().toLowerCase()
                : "";
        return "paid".equals(paymentStatus)
                || "success".equals(paymentStatus)
                || "completed".equals(paymentStatus);
    }

    private BigDecimal resolveOrderRefundAmount(Order order, boolean refundToWallet) {
        BigDecimal payable = order.getTotalAmount() != null && order.getTotalAmount() > 0
                ? BigDecimal.valueOf(order.getTotalAmount())
                : BigDecimal.ZERO;
        payable = payable.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal walletUsed = resolveWalletAmountUsedForOrder(order);
        if (refundToWallet) {
            return walletUsed.add(payable).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return walletUsed.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal resolveWalletAmountUsedForOrder(Order order) {
        if (order.getWalletAmountUsed() != null && order.getWalletAmountUsed() > 0) {
            return BigDecimal.valueOf(order.getWalletAmountUsed())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return walletService.getWalletDebitTotalForOrder(
                Math.toIntExact(order.getUserId()),
                order.getId()
        );
    }

    private void applyWalletAmountFields(
            Order order,
            OrderResponseDTO.OrderResponseDTOBuilder builder
    ) {
        double payable = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0d;
        double walletUsed = 0.0d;
        try {
            walletUsed = resolveWalletAmountUsedForOrder(order).doubleValue();
        } catch (Exception e) {
            log.warn(
                    "Wallet lookup failed for order id={}: {}",
                    order.getId(),
                    e.getMessage()
            );
            if (order.getWalletAmountUsed() != null && order.getWalletAmountUsed() > 0) {
                walletUsed = order.getWalletAmountUsed();
            }
        }
        builder
                .totalAmount(payable > 0 ? payable : null)
                .finalAmount(payable > 0 ? payable : null)
                .walletAmountUsed(walletUsed > 0 ? walletUsed : null)
                .grandTotal(
                        (payable + walletUsed) > 0.009d
                                ? Math.round((payable + walletUsed) * 100.0d) / 100.0d
                                : null
                );
    }


    @Override
    @Transactional
    public Order markOrderAsPaid(
            String razorpayOrderId,
            String paymentId
    ) {

        Order order =
                orderRepository
                        .findByRazorpayOrderId(
                                razorpayOrderId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found"
                                ));

        order.setPaymentStatus("paid");

        order.setRazorpayPaymentId(paymentId);

        if ("awaiting_payment".equalsIgnoreCase(
                order.getOrderStatus() != null ? order.getOrderStatus().trim() : "")) {
            order.setOrderStatus("processing");
        }

        try {
            processReferralAfterOrderPaid(order);
        } catch (Exception e) {
            log.error("Error processing referral on order payment: {}", e.getMessage(), e);
        }

        // Do not block payment verify on Shiprocket (avoids 15s client timeouts).
        scheduleShiprocketAfterCommit(order);

        order = orderRepository.save(order);
        try {
            clearCartSafely(order.getUserId());
        } catch (Exception e) {
            log.warn("Cart clear after payment failed for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
        sendOrderConfirmationEmailForOrder(order);
        return order;
    }

    @Override
    @Transactional
    public Order markOrderPaymentFailed(String razorpayOrderId) {
        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            throw new OrderException("Razorpay order id is required");
        }
        Order order = orderRepository
                .findByRazorpayOrderId(razorpayOrderId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        String paymentStatus = order.getPaymentStatus() != null
                ? order.getPaymentStatus().trim().toLowerCase(Locale.ENGLISH)
                : "";
        if ("paid".equals(paymentStatus)
                || "success".equals(paymentStatus)
                || "completed".equals(paymentStatus)
                || "captured".equals(paymentStatus)) {
            return order;
        }

        order.setPaymentStatus("failed");
        String orderStatus = order.getOrderStatus() != null
                ? order.getOrderStatus().trim().toLowerCase(Locale.ENGLISH)
                : "";
        if (orderStatus.isBlank()
                || "awaiting_payment".equals(orderStatus)
                || "pending".equals(orderStatus)
                || "processing".equals(orderStatus)) {
            order.setOrderStatus("payment_failed");
        }
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Push to Shiprocket only after DB commit, off the request thread.
     * Online unpaid orders are skipped until markOrderAsPaid.
     */
    private void scheduleShiprocketAfterCommit(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        final Long orderId = order.getId();
        final String orderNumber = order.getOrderNumber();

        Runnable push = () -> {
            try {
                Order fresh = orderRepository.findById(orderId).orElse(null);
                if (fresh == null) {
                    return;
                }
                if (fresh.getShiprocketOrderId() != null && !fresh.getShiprocketOrderId().isBlank()) {
                    return;
                }
                boolean isCod = isCodPaymentMethod(fresh.getPaymentMethod());
                String paymentStatus = fresh.getPaymentStatus() != null
                        ? fresh.getPaymentStatus().trim().toLowerCase()
                        : "";
                boolean paymentConfirmed = paymentStatus.equals("paid")
                        || paymentStatus.equals("completed")
                        || paymentStatus.equals("success")
                        || paymentStatus.equals("captured");
                if (!isCod && !paymentConfirmed) {
                    log.info("Skipping Shiprocket for unpaid online order {}", orderNumber);
                    return;
                }
                shiprocketService.createShipment(fresh);
                log.info("Shiprocket shipment created for order {}", orderNumber);
            } catch (Exception e) {
                log.error(
                        "Shiprocket shipment creation failed for order {} reason {}",
                        orderNumber,
                        e.getMessage(),
                        e
                );
                try {
                    markShiprocketCreateFailed(orderNumber, e.getMessage());
                } catch (Exception ignore) {
                    // ignore
                }
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            Thread t = new Thread(push, "shiprocket-push-" + orderId);
            t.setDaemon(true);
            t.start();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Thread t = new Thread(push, "shiprocket-push-" + orderId);
                t.setDaemon(true);
                t.start();
            }
        });
    }

    @Override
    @Transactional
    public ShiprocketShipmentResult pushOrderToShiprocket(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getShiprocketOrderId() != null && !order.getShiprocketOrderId().isBlank()) {
            return ShiprocketShipmentResult.builder()
                    .shipmentId(order.getShiprocketShipmentId())
                    .awbCode(order.getShiprocketAwbCode())
                    .trackingUrl(order.getShiprocketTrackingUrl())
                    .courierName(order.getShiprocketCourierName() != null
                            ? order.getShiprocketCourierName()
                            : "Shiprocket")
                    .build();
        }

        String paymentStatus = order.getPaymentStatus() != null
                ? order.getPaymentStatus().trim().toLowerCase()
                : "";
        boolean paymentConfirmed = paymentStatus.equals("paid")
                || paymentStatus.equals("completed")
                || paymentStatus.equals("success")
                || paymentStatus.equals("captured");
        boolean isCod = isCodPaymentMethod(order.getPaymentMethod());

        if (!isCod && !paymentConfirmed) {
            throw new OrderException(
                    "Order is not paid yet; Shiprocket push is only for paid or COD orders."
            );
        }

        ShiprocketShipmentResult result = shiprocketService.createShipment(order);
        log.info(
                "Shiprocket push retry OK orderNumber={} shipmentId={}",
                order.getOrderNumber(),
                result.getShipmentId()
        );
        return result;
    }

    private boolean isPendingOnlinePayment(Order order) {
        if (order == null || order.getPaymentStatus() == null) {
            return false;
        }
        // COD unpaid is collect-on-delivery — not an online Razorpay pending state.
        if (isCodPaymentMethod(order.getPaymentMethod())) {
            return false;
        }
        return "pending".equalsIgnoreCase(order.getPaymentStatus().trim())
                || "failed".equalsIgnoreCase(order.getPaymentStatus().trim());
    }

    private void sendOrderConfirmationEmailForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            List<OrderItemDTO> itemDTOList = orderItems.stream()
                    .map(this::buildOrderItemDTO)
                    .toList();

            User user = null;
            if (order.getUserId() != null) {
                user = userRepository.findById(order.getUserId()).orElse(null);
            }

            BigDecimal subtotal = BigDecimal.valueOf(sumItemSubtotal(itemDTOList));
            BigDecimal shipping = order.getShippingAmount() != null
                    ? BigDecimal.valueOf(order.getShippingAmount())
                    : BigDecimal.ZERO;
            BigDecimal discount = order.getDiscountAmount() != null
                    ? BigDecimal.valueOf(order.getDiscountAmount())
                    : BigDecimal.ZERO;
            BigDecimal walletApplied = resolveWalletAmountUsedForOrder(order);

            sendOrderConfirmationEmailSafely(
                    user,
                    order,
                    itemDTOList,
                    subtotal,
                    shipping,
                    discount,
                    walletApplied
            );
        } catch (Exception e) {
            log.warn(
                    "[EMAIL] order confirmation after payment skipped for order {}: {}",
                    order.getOrderNumber(),
                    e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public void updateShipment(
            String orderNumber,
            String awb,
            String courier,
            String trackingUrl,
            String shiprocketStatus
    ) {

        Order order =
                orderRepository
                        .findByOrderNumber(orderNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found"
                                ));

        order.setShiprocketAwbCode(awb);

        order.setShiprocketCourierName(courier);

        order.setShiprocketTrackingUrl(
                trackingUrl
        );

        order.setShiprocketStatus(
                shiprocketStatus
        );

        order.setShiprocketSyncedAt(
                LocalDateTime.now()
        );

        if ("awb_assigned".equalsIgnoreCase(
                shiprocketStatus
        )) {

            order.setOrderStatus(
                    "awb_assigned"
            );
        }

        orderRepository.save(order);

        log.info(
                "Shipment updated for order {}",
                orderNumber
        );
    }

    @Override
    @Transactional
    public void markShiprocketCreateFailed(
            String orderNumber,
            String reason
    ) {

        try {

            Order order =
                    orderRepository
                            .findByOrderNumber(orderNumber)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Order not found"
                                    ));

            order.setShiprocketStatus(
                    reason == null || reason.isBlank()
                            ? "shipment_creation_failed"
                            : ("failed: " + reason).substring(0, Math.min(50, ("failed: " + reason).length()))
            );

            orderRepository.save(order);

            log.error(
                    "Shiprocket creation failed for order {} reason {}",
                    orderNumber,
                    reason
            );

        } catch (Exception e) {

            log.error(
                    "Failed to mark Shiprocket failure",
                    e
            );
        }
    }

    private void processReferralAfterOrderPaid(Order order) {

        Long userId = order.getUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        if (user.getReferredBy() != null
                && !transactionRepository.existsByReferredUserIdAndTransactionType(
                        userId,
                        ReferralTransaction.TransactionType.referral_bonus)) {

            User referrer = userRepository.findById(user.getReferredBy()).orElse(null);

            if (referrer != null) {
                int prev = referrer.getReferralCount() != null ? referrer.getReferralCount() : 0;
                referrer.setReferralCount(prev + 1);

                int requiredReferrals = referralService.getRequiredReferralsForReward();
                if (referrer.getReferralCount() >= requiredReferrals) {
                    referrer.setRewardUnlocked(true);
                    referrer.setDiscountAvailable(true);
                    log.info(
                            "Referrer {} unlocked referral reward (count={}, required={})",
                            referrer.getId(),
                            referrer.getReferralCount(),
                            requiredReferrals
                    );
                }

                userRepository.save(referrer);

                ReferralTransaction tx = new ReferralTransaction();
                tx.setReferrerId(referrer.getId());
                tx.setReferredUserId(userId);
                tx.setAmount(BigDecimal.ZERO);
                tx.setTransactionType(ReferralTransaction.TransactionType.referral_bonus);
                tx.setStatus(ReferralTransaction.Status.completed);
                tx.setDescription("Friend's first paid order — referral counted");
                transactionRepository.save(tx);

                log.info(
                        "Referral count incremented for referrer {} by user {}",
                        referrer.getId(),
                        userId
                );
            }
        }

        if (Boolean.TRUE.equals(order.getReferralInviterDiscountApplied())) {
            user.setDiscountAvailable(false);
            user.setFirstOrderCompleted(true);
            userRepository.save(user);
            try {
                referralService.markReferralDiscountUsed(userId, order.getId());
            } catch (Exception e) {
                log.warn("markReferralDiscountUsed failed: {}", e.getMessage());
            }
        }
    }

    private Order createOrder(Long userId, PlaceOrderRequestDTO dto, Address address,
                              BigDecimal subtotal, BigDecimal shipping,
                              BigDecimal discount, BigDecimal finalAmount,
                              BigDecimal walletApplied,
                              boolean referralInviterDiscountApplied) {

        // Calculate tax (18% GST on subtotal)
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.18"));
        double walletUsed = walletApplied != null
                ? walletApplied.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0.0d;

        return Order.builder()
                .userId(userId)
                .orderNumber(generateOrderNumber())
                .totalAmount(finalAmount.doubleValue())
                .walletAmountUsed(walletUsed > 0 ? walletUsed : null)
                .shippingAmount(shipping.doubleValue())
                .taxAmount(taxAmount.doubleValue())
                .discountAmount(discount.doubleValue())
                .paymentMethod(dto.getPaymentMethod())
                .razorpayOrderId(dto.getRazorpayOrderId())
                .paymentStatus(
                        finalAmount.compareTo(BigDecimal.ZERO) <= 0
                                ? "paid"
                                : "pending"
                )
                .orderStatus(
                        finalAmount.compareTo(BigDecimal.ZERO) <= 0
                                || isCodPaymentMethod(dto.getPaymentMethod())
                                ? "processing"
                                : "awaiting_payment"
                )
                .shippingAddress1(address.getAddressLine1())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingCountry(
                        address.getCountry() != null
                                ? address.getCountry()
                                : "India"
                )
                .shippingPincode(address.getPincode())
                .shippingName(address.getName())
                .shippingPhone(address.getPhone())
                .shippingEmail(address.getEmail())
                .shippingAddress2(address.getAddressLine2())
                .billingName(address.getName())
                .billingPhone(address.getPhone())
                .billingEmail(address.getEmail())
                .billingAddress1(address.getAddressLine1())
                .billingAddress2(address.getAddressLine2())
                .billingCity(address.getCity())
                .billingState(address.getState())
                .billingCountry(
                        address.getCountry() != null
                                ? address.getCountry()
                                : "India"
                )
                .billingPincode(address.getPincode())
                .addressId(address.getId().longValue())
                .referralInviterDiscountApplied(referralInviterDiscountApplied)
                .build();
    }

    private void clearCartSafely(Long userId) {
        cartRepository.deleteByUser_Id(userId);
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

    private List<CartItemResponseDTO> resolveCheckoutCartItems(
            List<CartItemResponseDTO> allItems,
            List<Long> itemIds) {
        if (allItems == null || allItems.isEmpty()) {
            return List.of();
        }
        if (itemIds == null || itemIds.isEmpty()) {
            return allItems;
        }
        Set<Long> allowed = new HashSet<>();
        for (Long id : itemIds) {
            if (id != null && id > 0) {
                allowed.add(id);
            }
        }
        if (allowed.isEmpty()) {
            return allItems;
        }
        return allItems.stream()
                .filter(item -> item.getItemId() != null && allowed.contains(item.getItemId()))
                .toList();
    }

    private BigDecimal resolveCartLineTotal(CartItemResponseDTO item) {
        if (item.getTotal() != null) {
            return item.getTotal();
        }
        BigDecimal unit = item.getSellingPrice() != null
                ? item.getSellingPrice()
                : (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        return unit.multiply(BigDecimal.valueOf(Math.max(qty, 1)));
    }

    private BigDecimal resolveCartLineMrpTotal(CartItemResponseDTO item) {
        BigDecimal unitMrp = item.getMrpPrice() != null
                ? item.getMrpPrice()
                : (item.getOriginalPrice() != null ? item.getOriginalPrice() : null);
        if (unitMrp == null) {
            return resolveCartLineTotal(item);
        }
        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
        return unitMrp.multiply(BigDecimal.valueOf(Math.max(qty, 1)));
    }

    private BigDecimal sumCheckoutSubtotal(List<CartItemResponseDTO> items) {
        return items.stream()
                .map(this::resolveCartLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCheckoutDiscount(List<CartItemResponseDTO> items) {
        BigDecimal lineDiscountTotal = BigDecimal.ZERO;
        for (CartItemResponseDTO item : items) {
            BigDecimal lineTotal = resolveCartLineTotal(item);
            BigDecimal lineMrp = resolveCartLineMrpTotal(item);
            if (lineMrp.compareTo(lineTotal) > 0) {
                lineDiscountTotal = lineDiscountTotal.add(lineMrp.subtract(lineTotal));
            }
        }
        return lineDiscountTotal;
    }

    private List<OrderItemDTO> createOrderItems(
            Order order,
            List<CartItemResponseDTO> cartItems) {

        List<OrderItemDTO> list = new ArrayList<>();

        for (CartItemResponseDTO cartItem : cartItems) {

            Product product = productRepository.findById(
                    cartItem.getProductId()
            ).orElseThrow(() ->
                    new RuntimeException("Product not found"));

            ProductVariant variant = productVariantRepository.findById(
                    cartItem.getVariantId()
            ).orElseThrow(() ->
                    new RuntimeException("Variant not found"));

            double length =
                    product.getLengthCm() != null
                            ? product.getLengthCm().doubleValue()
                            : 1.0;

            double width =
                    product.getWidthCm() != null
                            ? product.getWidthCm().doubleValue()
                            : 1.0;

            double height =
                    product.getHeightCm() != null
                            ? product.getHeightCm().doubleValue()
                            : 1.0;

            double weight =
                    variant.getWeight() != null
                            ? variant.getWeight().doubleValue()
                            : (
                            product.getProductWeight() != null
                                    ? product.getProductWeight().doubleValue()
                                    : 0.5
                    );

            double volumetricWeight =
                    (length * width * height) / 5000.0;

            double chargeableWeight =
                    Math.max(weight, volumetricWeight);

            // Resolve product image path (cart URL first, then primary image from DB)
            String imagePath = null;
            if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isBlank()) {
                imagePath = cartItem.getImageUrl();
            } else {
                ProductImage primaryImg = productImageRepository.findTopByProductIdAndIsPrimaryTrue(product.getId());
                if (primaryImg != null && primaryImg.getImagePath() != null) {
                    imagePath = primaryImg.getImagePath();
                } else {
                    Optional<ProductImage> firstImg = productImageRepository.findFirstByProductId(product.getId());
                    if (firstImg.isPresent() && firstImg.get().getImagePath() != null) {
                        imagePath = firstImg.get().getImagePath();
                    }
                }
            }

            OrderItem item = OrderItem.builder()

                    .orderId(order.getId())

                    .productId(product.getId())

                    .variantId(variant.getId())

                    .productName(product.getName())

                    .productImagePath(imagePath)

                    .color(variant.getColor())

                    .size(variant.getSize())

                    .sku(variant.getSku())

                    .weight(weight)

                    .lengthCm(length)

                    .widthCm(width)

                    .heightCm(height)

                    .packageDeadWeight(weight)

                    .volumetricWeight(volumetricWeight)

                    .chargeableWeight(chargeableWeight)

                    .sellerId(product.getSellerId())

                    .sellerName("FNT Seller")

                    .quantity(cartItem.getQuantity())

                    .price(cartItem.getPrice().doubleValue())

                    .mrpPrice(cartItem.getMrpPrice() != null ? cartItem.getMrpPrice().doubleValue() : null)

                    .total(cartItem.getTotal().doubleValue())

                    .hsnCode(product.getHsnCode() != null ? product.getHsnCode() : "0000")

                    .status("processing")

                    .build();

            orderItemRepository.save(item);

            list.add(buildOrderItemDTO(item));
        }

        return list;
    }

    private OrderItemDTO buildOrderItemDTO(OrderItem item) {

        String resolvedName = item.getProductName();
        String resolvedImage = item.getProductImagePath();
        String resolvedColor = item.getColor();
        String resolvedSize = item.getSize();
        String resolvedSku = item.getSku();

        // Fallback for old orders that didn't store productName/image/color/size
        if ((resolvedName == null || resolvedName.isBlank())
                || (resolvedImage == null || resolvedImage.isBlank())) {
            try {
                if (resolvedName == null || resolvedName.isBlank()) {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    if (product != null) {
                        resolvedName = product.getName();
                    }
                }
                if (resolvedImage == null || resolvedImage.isBlank()) {
                    ProductImage primaryImg = productImageRepository.findTopByProductIdAndIsPrimaryTrue(item.getProductId());
                    if (primaryImg != null && primaryImg.getImagePath() != null) {
                        resolvedImage = primaryImg.getImagePath();
                    } else {
                        Optional<ProductImage> firstImg = productImageRepository.findFirstByProductId(item.getProductId());
                        if (firstImg.isPresent() && firstImg.get().getImagePath() != null) {
                            resolvedImage = firstImg.get().getImagePath();
                        }
                    }
                }
                if ((resolvedColor == null || resolvedColor.isBlank())
                        || (resolvedSize == null || resolvedSize.isBlank())
                        || (resolvedSku == null || resolvedSku.isBlank())) {
                    if (item.getVariantId() != null) {
                        ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                        if (variant != null) {
                            if (resolvedColor == null || resolvedColor.isBlank()) resolvedColor = variant.getColor();
                            if (resolvedSize == null || resolvedSize.isBlank()) resolvedSize = variant.getSize();
                            if (resolvedSku == null || resolvedSku.isBlank()) resolvedSku = variant.getSku();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not resolve product details for order item id={}: {}", item.getId(), e.getMessage());
            }
        }

        OrderItemDTO.OrderItemDTOBuilder builder = OrderItemDTO.builder()

                .orderItemId(item.getId())

                .productId(item.getProductId())

                .variantId(item.getVariantId())

                .productName(resolvedName)

                .productImage(resolvedImage)

                .sku(resolvedSku)

                .hsnCode(item.getHsnCode())

                .color(resolvedColor)

                .size(resolvedSize)

                .weight(item.getWeight())

                .lengthCm(item.getLengthCm())

                .widthCm(item.getWidthCm())

                .heightCm(item.getHeightCm())

                .packageDeadWeight(item.getPackageDeadWeight())

                .volumetricWeight(item.getVolumetricWeight())

                .chargeableWeight(item.getChargeableWeight())

                .sellerName(item.getSellerName())

                .sellerId(item.getSellerId())

                .quantity(item.getQuantity())

                .price(item.getPrice())

                .mrpPrice(item.getMrpPrice())

                .total(item.getTotal())

                .returnPolicy(resolveProductReturnPolicy(item.getProductId()));

        orderItemCustomDetailService.enrichOrderItemDto(item, builder);
        return builder.build();
    }

    private String resolveProductReturnPolicy(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }
        try {
            return productRepository.findById(productId)
                    .map(Product::getReturnPolicy)
                    .map(policy -> policy != null ? policy.trim() : null)
                    .filter(policy -> !policy.isEmpty())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve return policy for productId={}: {}", productId, e.getMessage());
            return null;
        }
    }

    private OrderResponseDTO buildOrderResponse(
            Order order,
            List<OrderItemDTO> items
    ) {
        double totalWeight = items.stream()

                .map(OrderItemDTO::getWeight)

                .filter(Objects::nonNull)

                .mapToDouble(Double::doubleValue)

                .sum();

        OrderResponseDTO.OrderResponseDTOBuilder responseBuilder = OrderResponseDTO.builder()

                .orderId(order.getId())

                .orderNumber(order.getOrderNumber())

                .orderStatus(order.getOrderStatus())

                .paymentMethod(order.getPaymentMethod())

                .paymentStatus(order.getPaymentStatus())

                .shippingAmount(order.getShippingAmount())

                .discountAmount(order.getDiscountAmount())

                .createdDate(formatOrderCreatedDate(order.getCreatedAt()))

                .totalWeight(totalWeight)

                .items(items)

                .shiprocketAwbCode(
                        order.getShiprocketAwbCode()
                )

                .shiprocketCourierName(
                        order.getShiprocketCourierName()
                )

                .shiprocketTrackingUrl(
                        order.getShiprocketTrackingUrl()
                )

                .shiprocketStatus(
                        order.getShiprocketStatus()
                );

        applyOrderResponseFields(order, responseBuilder);
        return responseBuilder.build();
    }

    private OrderResponseDTO buildOrderSummaryResponseLite(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemDTO> itemDTOList = items.stream()
                .map(this::buildOrderItemDTO)
                .toList();
        int totalQuantity = items.stream()
                .mapToInt(item -> item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1)
                .sum();

        String firstImage = null;
        if (!itemDTOList.isEmpty() && itemDTOList.get(0).getProductImage() != null) {
            firstImage = itemDTOList.get(0).getProductImage();
        } else if (!items.isEmpty()) {
            OrderItem first = items.get(0);
            firstImage = first.getProductImagePath();
            if (firstImage == null || firstImage.isBlank()) {
                try {
                    ProductImage primaryImg =
                            productImageRepository.findTopByProductIdAndIsPrimaryTrue(first.getProductId());
                    if (primaryImg != null && primaryImg.getImagePath() != null) {
                        firstImage = primaryImg.getImagePath();
                    }
                } catch (Exception e) {
                    log.warn(
                            "Could not resolve list image for order id={}: {}",
                            order.getId(),
                            e.getMessage()
                    );
                }
            }
        }

        OrderResponseDTO.OrderResponseDTOBuilder summaryBuilder = OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .shippingAmount(order.getShippingAmount())
                .discountAmount(order.getDiscountAmount())
                .createdDate(formatOrderCreatedDate(order.getCreatedAt()))
                .totalItems(items.size())
                .totalQuantity(totalQuantity > 0 ? totalQuantity : items.size())
                .firstProductImage(firstImage)
                .items(itemDTOList)
                .shiprocketAwbCode(order.getShiprocketAwbCode())
                .shiprocketCourierName(order.getShiprocketCourierName())
                .shiprocketTrackingUrl(order.getShiprocketTrackingUrl())
                .shiprocketStatus(order.getShiprocketStatus());

        applyWalletAmountFields(order, summaryBuilder);
        return summaryBuilder.build();
    }

    private OrderResponseDTO buildOrderSummaryResponse(Order order) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        double totalWeight = items.stream()
                .map(OrderItem::getWeight)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        List<OrderItemDTO> itemDTOList = items.stream()
                .map(this::buildOrderItemDTO)
                .toList();

        // Use resolved image from DTO (handles old orders without stored image)
        String firstImage = null;
        if (!itemDTOList.isEmpty() && itemDTOList.get(0).getProductImage() != null) {
            firstImage = itemDTOList.get(0).getProductImage();
        }

        OrderResponseDTO.OrderResponseDTOBuilder summaryBuilder = OrderResponseDTO.builder()

                .orderId(order.getId())

                .orderNumber(order.getOrderNumber())

                .orderStatus(order.getOrderStatus())

                .paymentMethod(order.getPaymentMethod())

                .paymentStatus(order.getPaymentStatus())

                .shippingAmount(order.getShippingAmount())

                .discountAmount(order.getDiscountAmount())

                .createdDate(formatOrderCreatedDate(order.getCreatedAt()))

                .totalWeight(totalWeight)

                .totalItems(items.size())

                .totalQuantity(items.stream()
                        .mapToInt(item -> item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1)
                        .sum())

                .firstProductImage(firstImage)

                .items(itemDTOList)

                .shiprocketAwbCode(order.getShiprocketAwbCode())

                .shiprocketCourierName(
                        order.getShiprocketCourierName())

                .shiprocketTrackingUrl(
                        order.getShiprocketTrackingUrl())

                .shiprocketStatus(
                        order.getShiprocketStatus());

        applyOrderResponseFields(order, summaryBuilder);
        return summaryBuilder.build();
    }


    private OrderResponseDTO buildDetailedOrderResponse(
            Order order,
            List<OrderItemDTO> items
    ) {

        double totalWeight = items.stream()

                .map(OrderItemDTO::getWeight)

                .filter(Objects::nonNull)

                .mapToDouble(Double::doubleValue)

                .sum();
        OrderResponseDTO.OrderResponseDTOBuilder detailBuilder = OrderResponseDTO.builder()

                .orderId(order.getId())

                .orderNumber(order.getOrderNumber())

                .orderStatus(order.getOrderStatus())

                .paymentMethod(order.getPaymentMethod())

                .paymentStatus(order.getPaymentStatus())

                .shippingAmount(order.getShippingAmount())

                .discountAmount(order.getDiscountAmount())

                .createdDate(formatOrderCreatedDate(order.getCreatedAt()))

                .totalWeight(totalWeight)

                .items(items)

                .shiprocketAwbCode(
                        order.getShiprocketAwbCode()
                )

                .shiprocketCourierName(
                        order.getShiprocketCourierName()
                )

                .shiprocketTrackingUrl(
                        order.getShiprocketTrackingUrl()
                )

                .shiprocketStatus(
                        order.getShiprocketStatus()
                );

        applyOrderResponseFields(order, detailBuilder);
        return detailBuilder.build();
    }

    private void applyOrderResponseFields(
            Order order,
            OrderResponseDTO.OrderResponseDTOBuilder builder
    ) {
        applyWalletAmountFields(order, builder);
        applyShippingContactFields(order, builder);
        applyStatusTimelineFields(order, builder);
        builder
                .taxAmount(order.getTaxAmount())
                .razorpayPaymentId(order.getRazorpayPaymentId());
    }

    private void applyStatusTimelineFields(
            Order order,
            OrderResponseDTO.OrderResponseDTOBuilder builder
    ) {
        if (order == null || order.getId() == null) {
            return;
        }

        String placedAt = formatOrderCreatedDate(order.getCreatedAt());
        String confirmedAt = null;
        String shippedAt = null;
        String outForDeliveryAt = null;
        String deliveredAt = null;
        String cancelledAt = null;

        List<OrderStatusHistoryDTO> historyDtos = new ArrayList<>();
        try {
            List<OrderStatusHistory> historyRows =
                    orderStatusHistoryRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId());
            for (OrderStatusHistory row : historyRows) {
                if (row == null || row.getStatus() == null) {
                    continue;
                }
                String statusName = row.getStatus().name();
                String at = formatOrderCreatedDate(row.getCreatedAt());
                historyDtos.add(OrderStatusHistoryDTO.builder()
                        .status(statusName)
                        .comment(row.getComment())
                        .createdAt(at)
                        .build());

                switch (row.getStatus()) {
                    case CREATED, PLACED -> {
                        if (placedAt == null) {
                            placedAt = at;
                        }
                    }
                    case CONFIRMED, PACKED -> {
                        if (confirmedAt == null) {
                            confirmedAt = at;
                        }
                    }
                    case SHIPPED -> {
                        if (shippedAt == null) {
                            shippedAt = at;
                        }
                    }
                    case OUT_FOR_DELIVERY -> {
                        if (outForDeliveryAt == null) {
                            outForDeliveryAt = at;
                        }
                    }
                    case DELIVERED -> {
                        if (deliveredAt == null) {
                            deliveredAt = at;
                        }
                    }
                    case CANCELLED -> {
                        if (cancelledAt == null) {
                            cancelledAt = at;
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Could not load order_status_history for orderId={}: {}",
                    order.getId(),
                    e.getMessage()
            );
        }

        String orderStatus = order.getOrderStatus() != null
                ? order.getOrderStatus().trim().toLowerCase(Locale.ENGLISH)
                : "";
        // Fallback: if marked delivered/cancelled but history has no stamp, use updatedAt
        // only when it differs from createdAt (avoids fake "delivered = placed" labels).
        if (deliveredAt == null && orderStatus.contains("deliver")) {
            String updated = formatOrderCreatedDate(order.getUpdatedAt());
            if (updated != null && !updated.equals(placedAt)) {
                deliveredAt = updated;
            }
        }
        if (cancelledAt == null && orderStatus.contains("cancel")) {
            String updated = formatOrderCreatedDate(order.getUpdatedAt());
            if (updated != null && !updated.equals(placedAt)) {
                cancelledAt = updated;
            }
        }
        if (shippedAt == null && (orderStatus.contains("ship") || orderStatus.contains("transit"))) {
            String updated = formatOrderCreatedDate(order.getUpdatedAt());
            if (updated != null && !updated.equals(placedAt)) {
                shippedAt = updated;
            }
        }

        builder
                .placedAt(placedAt)
                .confirmedAt(confirmedAt)
                .shippedAt(shippedAt)
                .outForDeliveryAt(outForDeliveryAt)
                .deliveredAt(deliveredAt)
                .cancelledAt(cancelledAt)
                .statusHistory(historyDtos);
    }

    private void applyShippingContactFields(
            Order order,
            OrderResponseDTO.OrderResponseDTOBuilder builder
    ) {
        builder
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingEmail(order.getShippingEmail())
                .shippingAddress1(order.getShippingAddress1())
                .shippingAddress2(order.getShippingAddress2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPincode(order.getShippingPincode())
                .shippingCountry(
                        order.getShippingCountry() != null
                                ? order.getShippingCountry()
                                : "India"
                )
                .shippingAddress(formatShippingAddress(order))
                .billingName(order.getBillingName())
                .billingPhone(order.getBillingPhone())
                .billingEmail(order.getBillingEmail())
                .billingAddress1(order.getBillingAddress1())
                .billingAddress2(order.getBillingAddress2())
                .billingCity(order.getBillingCity())
                .billingState(order.getBillingState())
                .billingPincode(order.getBillingPincode())
                .billingCountry(
                        order.getBillingCountry() != null
                                ? order.getBillingCountry()
                                : "India"
                )
                .billingAddress(formatBillingAddress(order));
    }

    private String formatBillingAddress(Order order) {
        if (order == null) {
            return "";
        }
        if (isBlank(order.getBillingName())
                && isBlank(order.getBillingAddress1())
                && isBlank(order.getBillingCity())) {
            return formatShippingAddress(order);
        }
        List<String> lines = new ArrayList<>();
        if (!isBlank(order.getBillingName())) {
            lines.add(order.getBillingName().trim());
        }
        if (!isBlank(order.getBillingAddress1())) {
            lines.add(order.getBillingAddress1().trim());
        }
        if (!isBlank(order.getBillingAddress2())) {
            lines.add(order.getBillingAddress2().trim());
        }
        StringBuilder cityLine = new StringBuilder();
        if (!isBlank(order.getBillingCity())) {
            cityLine.append(order.getBillingCity().trim());
        }
        if (!isBlank(order.getBillingState())) {
            if (!cityLine.isEmpty()) {
                cityLine.append(", ");
            }
            cityLine.append(order.getBillingState().trim());
        }
        if (!isBlank(order.getBillingPincode())) {
            if (!cityLine.isEmpty()) {
                cityLine.append(" - ");
            }
            cityLine.append(order.getBillingPincode().trim());
        }
        if (!cityLine.isEmpty()) {
            lines.add(cityLine.toString());
        }
        if (!isBlank(order.getBillingCountry())) {
            lines.add(order.getBillingCountry().trim());
        }
        return String.join("\n", lines);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String formatShippingAddress(Order order) {
        if (order == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        if (order.getShippingName() != null && !order.getShippingName().isBlank()) {
            lines.add(order.getShippingName().trim());
        }
        if (order.getShippingAddress1() != null && !order.getShippingAddress1().isBlank()) {
            lines.add(order.getShippingAddress1().trim());
        }
        if (order.getShippingAddress2() != null && !order.getShippingAddress2().isBlank()) {
            lines.add(order.getShippingAddress2().trim());
        }
        StringBuilder cityLine = new StringBuilder();
        if (order.getShippingCity() != null && !order.getShippingCity().isBlank()) {
            cityLine.append(order.getShippingCity().trim());
        }
        if (order.getShippingState() != null && !order.getShippingState().isBlank()) {
            if (!cityLine.isEmpty()) {
                cityLine.append(", ");
            }
            cityLine.append(order.getShippingState().trim());
        }
        if (order.getShippingPincode() != null && !order.getShippingPincode().isBlank()) {
            if (!cityLine.isEmpty()) {
                cityLine.append(" - ");
            }
            cityLine.append(order.getShippingPincode().trim());
        }
        if (!cityLine.isEmpty()) {
            lines.add(cityLine.toString());
        }
        if (order.getShippingCountry() != null && !order.getShippingCountry().isBlank()) {
            lines.add(order.getShippingCountry().trim());
        }
        if (order.getShippingPhone() != null && !order.getShippingPhone().isBlank()) {
            lines.add("Phone: " + order.getShippingPhone().trim());
        }
        return String.join("\n", lines);
    }

    private void sendOrderConfirmationEmailSafely(
            User user,
            Order order,
            List<OrderItemDTO> items,
            BigDecimal subtotal,
            BigDecimal shipping,
            BigDecimal discount,
            BigDecimal walletApplied
    ) {
        try {
            String customerRecipient = firstNonBlank(
                    user != null ? user.getEmail() : null,
                    order.getShippingEmail()
            );
            if (customerRecipient == null) {
                log.warn("[EMAIL] No customer email for order {}", order.getOrderNumber());
            } else {
                OrderConfirmationEmailModel customerModel = buildOrderEmailModel(
                        user,
                        order,
                        items,
                        subtotal,
                        shipping,
                        discount,
                        walletApplied,
                        OrderConfirmationEmailModel.RECIPIENT_CUSTOMER,
                        null,
                        customerRecipient,
                        buildOrderViewUrl(order)
                );

                Integer userIdInt = null;
                if (user != null && user.getId() != null && user.getId() <= Integer.MAX_VALUE) {
                    userIdInt = user.getId().intValue();
                }
                emailService.sendOrderConfirmationEmail(userIdInt, customerModel);
            }

            sendSellerOrderNotificationEmails(user, order, items);
            sendAdminOrderNotificationEmails(user, order, items, subtotal, shipping, discount, walletApplied);
        } catch (Exception e) {
            log.warn(
                    "[EMAIL] order confirmation skipped for order {}: {}",
                    order.getOrderNumber(),
                    e.getMessage()
            );
        }
    }

    private void sendSellerOrderNotificationEmails(
            User user,
            Order order,
            List<OrderItemDTO> items
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<Long, List<OrderItemDTO>> itemsBySeller = new LinkedHashMap<>();
        for (OrderItemDTO item : items) {
            if (item.getSellerId() == null || item.getSellerId() <= 0) {
                continue;
            }
            itemsBySeller.computeIfAbsent(item.getSellerId(), ignored -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<Long, List<OrderItemDTO>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<OrderItemDTO> sellerItems = entry.getValue();
            Seller seller = sellerRepository.findById(sellerId).orElse(null);
            String sellerEmail = seller != null ? firstNonBlank(seller.getEmail()) : null;
            if (sellerEmail == null) {
                log.warn("[EMAIL] No seller email for sellerId {} on order {}", sellerId, order.getOrderNumber());
                continue;
            }

            String sellerName = resolveSellerDisplayName(seller, sellerItems);
            // Same layout as customer; amounts match only this seller's items.
            BigDecimal sellerSubtotal = BigDecimal.valueOf(sumItemSubtotal(sellerItems));
            OrderConfirmationEmailModel sellerModel = buildOrderEmailModel(
                    user,
                    order,
                    sellerItems,
                    sellerSubtotal,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    OrderConfirmationEmailModel.RECIPIENT_SELLER,
                    sellerName,
                    sellerEmail,
                    buildSellerOrderViewUrl(order)
            );
            emailService.sendOrderConfirmationEmail(null, sellerModel);
        }
    }

    private void sendAdminOrderNotificationEmails(
            User user,
            Order order,
            List<OrderItemDTO> items,
            BigDecimal subtotal,
            BigDecimal shipping,
            BigDecimal discount,
            BigDecimal walletApplied
    ) {
        LinkedHashSet<String> adminRecipients = new LinkedHashSet<>();
        try {
            adminUserRepository.findByStatus(AdminStatus.ACTIVE).stream()
                    .map(AdminUser::getEmail)
                    .map(this::firstNonBlank)
                    .filter(Objects::nonNull)
                    .forEach(adminRecipients::add);
        } catch (Exception e) {
            log.warn("[EMAIL] Could not load admin recipients: {}", e.getMessage());
        }

        String fallbackAdminEmail = firstNonBlank(adminNotifyEmail);
        if (fallbackAdminEmail != null) {
            adminRecipients.add(fallbackAdminEmail);
        }

        if (adminRecipients.isEmpty()) {
            log.warn("[EMAIL] No admin recipients configured for order {}", order.getOrderNumber());
            return;
        }

        for (String adminEmail : adminRecipients) {
            OrderConfirmationEmailModel adminModel = buildOrderEmailModel(
                    user,
                    order,
                    items,
                    subtotal,
                    shipping,
                    discount,
                    walletApplied,
                    OrderConfirmationEmailModel.RECIPIENT_ADMIN,
                    "Admin Team",
                    adminEmail,
                    buildAdminOrderViewUrl(order)
            );
            emailService.sendOrderConfirmationEmail(null, adminModel);
        }
    }

    private OrderConfirmationEmailModel buildOrderEmailModel(
            User user,
            Order order,
            List<OrderItemDTO> items,
            BigDecimal subtotal,
            BigDecimal shipping,
            BigDecimal discount,
            BigDecimal walletApplied,
            String recipientType,
            String recipientName,
            String recipientEmail,
            String orderViewUrl
    ) {
        double subtotalVal = subtotal != null
                ? subtotal.doubleValue()
                : sumItemSubtotal(items);
        double shippingVal = shipping != null ? shipping.doubleValue() : safeDouble(order.getShippingAmount());
        double discountVal = discount != null ? discount.doubleValue() : safeDouble(order.getDiscountAmount());
        double walletVal = walletApplied != null
                ? walletApplied.doubleValue()
                : safeDouble(order.getWalletAmountUsed());
        double payable;
        double grandTotal;
        if (OrderConfirmationEmailModel.RECIPIENT_SELLER.equals(recipientType)) {
            // Seller mail uses same layout; totals are for this seller's items only.
            grandTotal = Math.max(0.0d, subtotalVal - discountVal + shippingVal);
            payable = Math.max(0.0d, grandTotal - walletVal);
        } else {
            payable = safeDouble(order.getTotalAmount());
            grandTotal = payable + walletVal;
        }

        String customerName = firstNonBlank(
                order.getShippingName(),
                user != null ? user.getUsername() : null,
                "Customer"
        );

        return new OrderConfirmationEmailModel(
                customerName,
                recipientEmail,
                order.getOrderNumber(),
                formatOrderCreatedDate(order.getCreatedAt()),
                formatPaymentMethodLabel(order.getPaymentMethod(), walletVal, payable),
                formatPaymentStatusLabel(order.getPaymentMethod(), order.getPaymentStatus()),
                subtotalVal,
                discountVal,
                shippingVal,
                walletVal,
                grandTotal,
                payable,
                formatShippingAddress(order),
                orderViewUrl,
                items,
                recipientType,
                recipientName != null ? recipientName : customerName
        );
    }

    private String resolveSellerDisplayName(Seller seller, List<OrderItemDTO> sellerItems) {
        if (seller != null) {
            String businessName = firstNonBlank(seller.getBusinessName());
            if (businessName != null) {
                return businessName;
            }
            String fullName = firstNonBlank(
                    joinName(seller.getFirstName(), seller.getLastName()),
                    seller.getEmail()
            );
            if (fullName != null) {
                return fullName;
            }
        }
        if (sellerItems != null && !sellerItems.isEmpty()) {
            String itemSellerName = firstNonBlank(sellerItems.get(0).getSellerName());
            if (itemSellerName != null) {
                return itemSellerName;
            }
        }
        return "Seller";
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isBlank() ? null : combined;
    }

    private String buildSellerOrderViewUrl(Order order) {
        String base = sellerFrontendBaseUrl != null
                ? sellerFrontendBaseUrl.replaceAll("/+$", "")
                : "https://flintnthread.in";
        return base + "/orderDetails?orderId=" + order.getId();
    }

    private String buildAdminOrderViewUrl(Order order) {
        String base = adminFrontendBaseUrl != null
                ? adminFrontendBaseUrl.replaceAll("/+$", "")
                : "https://flintnthread.in";
        return base + "/orders/" + order.getId();
    }

    private String buildOrderViewUrl(Order order) {
        String base = publicWebBaseUrl != null
                ? publicWebBaseUrl.replaceAll("/+$", "")
                : "https://flintnthread.in";
        String orderNumber = order.getOrderNumber() != null ? order.getOrderNumber() : "";
        return base + "/order-view.html?orderId=" + order.getId()
                + "&orderNumber=" + java.net.URLEncoder.encode(
                orderNumber,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    private double sumItemSubtotal(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return 0.0d;
        }
        return items.stream()
                .mapToDouble(item -> {
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    if (item.getTotal() != null) {
                        return item.getTotal();
                    }
                    double price = item.getPrice() != null ? item.getPrice() : 0.0d;
                    return price * qty;
                })
                .sum();
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0d;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String formatPaymentMethodLabel(String method, double walletUsed, double payable) {
        String base = formatPaymentMethodOnly(method);
        if (walletUsed <= 0.009) {
            return base;
        }
        if (payable <= 0.009) {
            return "FNT Wallet";
        }
        if ("FNT Wallet".equalsIgnoreCase(base)) {
            return "FNT Wallet";
        }
        return base + " + FNT Wallet";
    }

    private String formatPaymentMethodOnly(String method) {
        if (method == null || method.isBlank()) {
            return "Online";
        }
        String normalized = method.trim().toLowerCase(Locale.ENGLISH).replace('-', '_');
        if (normalized.contains("cod") || normalized.contains("cash")) {
            return "COD";
        }
        if (normalized.equals("upi")) {
            return "UPI";
        }
        if (normalized.contains("card")) {
            return "Card";
        }
        return method.trim();
    }

    private String formatPaymentStatusLabel(String paymentMethod, String status) {
        String normalized = status != null
                ? status.trim().toLowerCase(Locale.ENGLISH)
                : "";
        boolean paid = normalized.equals("paid")
                || normalized.equals("success")
                || normalized.equals("completed")
                || normalized.equals("captured");
        if (isCodPaymentMethod(paymentMethod) && !paid) {
            return "Pending (COD)";
        }
        if (status == null || status.isBlank()) {
            return "Pending";
        }
        if (paid) {
            return "Paid";
        }
        if (normalized.equals("pending")) {
            return "Pending";
        }
        if (normalized.equals("failed")) {
            return "Failed";
        }
        return status.substring(0, 1).toUpperCase(Locale.ENGLISH) + status.substring(1);
    }

    private String formatOrderCreatedDate(LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }
        return createdAt
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ORDER_DISPLAY_ZONE)
                .format(ORDER_CREATED_DISPLAY_FORMAT);
    }

    private void validatePlaceOrderRequest(PlaceOrderRequestDTO dto) {
        if (dto == null || dto.getAddressId() == null) {
            throw new OrderException("Invalid request");
        }
    }

    @Override
    @Transactional
    public void linkRazorpayOrder(
            String razorpayOrderId
    ) {

        log.info(
                "Linked Razorpay order {}",
                razorpayOrderId
        );
    }

    @Override
    @Transactional
    public void updateOrderStatusFromWebhook(
            String awb,
            String status
    ) {

        try {

            Order order =
                    orderRepository
                            .findByShiprocketAwbCode(awb)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Order not found"
                                    ));

            order.setShiprocketStatus(status);

            String normalized =
                    status.toLowerCase()
                            .replace(" ", "_");

            switch (normalized) {

                case "awb_assigned":
                    order.setOrderStatus(
                            "awb_assigned"
                    );
                    break;

                case "pickup_scheduled":
                    order.setOrderStatus(
                            "pickup_scheduled"
                    );
                    break;

                case "picked_up":
                    order.setOrderStatus(
                            "picked_up"
                    );
                    break;

                case "in_transit":
                case "shipped":

                    order.setOrderStatus(
                            "in_transit"
                    );

                    User shippedUser =
                            userRepository
                                    .findById(order.getUserId())
                                    .orElse(null);

                    if (shippedUser != null) {
                        pushNotificationService.notifyUser(
                                order.getUserId(),
                                "Order #" + order.getOrderNumber() + " shipped",
                                "Your order is now shipped.",
                                "order",
                                "/orders?orderId=" + order.getId()
                        );
                    }

                    break;

                case "out_for_delivery":
                    order.setOrderStatus(
                            "out_for_delivery"
                    );
                    break;

                case "delivered":

                    order.setOrderStatus(
                            "delivered"
                    );

                    User deliveredUser =
                            userRepository
                                    .findById(order.getUserId())
                                    .orElse(null);

                    if (deliveredUser != null) {
                        pushNotificationService.notifyUser(
                                order.getUserId(),
                                "Order #" + order.getOrderNumber() + " has been delivered",
                                "Good news! Your order has been delivered successfully.",
                                "order",
                                "/orders?orderId=" + order.getId()
                        );
                    }

                    break;

                case "rto":
                    order.setOrderStatus(
                            "rto_initiated"
                    );
                    break;

                case "rto_initiated":
                    order.setOrderStatus(
                            "rto_initiated"
                    );
                    break;

                case "rto_delivered":
                    order.setOrderStatus(
                            "rto_delivered"
                    );
                    break;

                case "cancelled":
                    order.setOrderStatus(
                            "cancelled"
                    );
                    break;

                case "returned":
                    order.setOrderStatus(
                            "returned"
                    );
                    break;

                case "replacement":
                    order.setOrderStatus(
                            "replacement"
                    );
                    break;
            }

            order.setShiprocketSyncedAt(
                    LocalDateTime.now()
            );

            orderRepository.save(order);

            log.info(
                    "Webhook updated order {} status {}",
                    order.getOrderNumber(),
                    status
            );

        } catch (Exception e) {

            log.error(
                    "Webhook update failed",
                    e
            );
        }
    }

    private String generateOrderNumber() {
        // Format: FNT + YYYYMMDD + 4 random digits
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        int randomNum = 1000 + random.nextInt(9000); // 4-digit random number
        return "FNT" + date + randomNum;
    }
    @Override
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Override
    public RetryPaymentResponseDTO retryPayment(
            Long orderId
    ) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                ));

        assertCanAccessOrder(order);

        if (
                "paid".equalsIgnoreCase(
                        order.getPaymentStatus()
                )
        ) {

            throw new RuntimeException(
                    "Order already paid"
            );
        }

        Double amount =
                order.getTotalAmount();

        JSONObject razorpayOrder =
                razorpayService
                        .createOrder(amount);

        String razorpayOrderId =
                razorpayOrder.getString("id");

        order.setRazorpayOrderId(
                razorpayOrderId
        );

        orderRepository.save(order);

        return RetryPaymentResponseDTO
                .builder()
                .razorpayOrderId(
                        razorpayOrderId
                )
                .amount(amount)
                .currency("INR")
                .key("YOUR_RAZORPAY_KEY")
                .orderNumber(
                        order.getOrderNumber()
                )
                .build();
    }

    @Override
    @Transactional
    public boolean verifyRetryPayment(
            VerifyPaymentRequestDTO dto
    ) {

        boolean verified =
                razorpayService.verifyPayment(
                        dto.getRazorpayOrderId(),
                        dto.getRazorpayPaymentId(),
                        dto.getRazorpaySignature()
                );

        if (!verified) {

            throw new RuntimeException(
                    "Payment verification failed"
            );
        }

        Order order =
                orderRepository
                        .findById(dto.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found"
                                ));

        assertCanAccessOrder(order);

        boolean shouldSendConfirmationEmail = isPendingOnlinePayment(order);

        order.setPaymentStatus("PAID");

        order.setOrderStatus("processing");

        order.setRazorpayPaymentId(
                dto.getRazorpayPaymentId()
        );

        order = orderRepository.save(order);
        if (shouldSendConfirmationEmail) {
            sendOrderConfirmationEmailForOrder(order);
        }

        return true;
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderAddress(Long orderId, UpdateOrderAddressRequestDTO dto) {
        if (dto == null || dto.getShipping() == null) {
            throw new OrderException("Shipping address is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        assertCanAccessOrder(order);

        String status = order.getOrderStatus() != null
                ? order.getOrderStatus().toLowerCase(Locale.ROOT).trim()
                : "";

        List<String> blockedStatuses = Arrays.asList(
                "delivered",
                "out_for_delivery",
                "picked_up",
                "in_transit",
                "rto_delivered",
                "cancelled",
                "shipped"
        );
        if (blockedStatuses.contains(status)) {
            throw new OrderException("Address cannot be changed for this order status");
        }
        if (order.getShiprocketAwbCode() != null && !order.getShiprocketAwbCode().isBlank()) {
            throw new OrderException("Address cannot be changed after shipment has been created");
        }

        UpdateOrderAddressRequestDTO.OrderAddressSectionDTO shipping = dto.getShipping();
        applyShippingSection(order, shipping);

        boolean sameAsShipping = dto.getBilling() == null
                || Boolean.TRUE.equals(dto.getBillingSameAsShipping());
        UpdateOrderAddressRequestDTO.OrderAddressSectionDTO billing =
                sameAsShipping ? shipping : dto.getBilling();
        applyBillingSection(order, billing);

        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        notifySellersOfAddressUpdate(order, items);

        List<OrderItemDTO> itemDTOList = items.stream().map(this::buildOrderItemDTO).toList();
        return buildDetailedOrderResponse(order, itemDTOList);
    }

    private void applyShippingSection(
            Order order,
            UpdateOrderAddressRequestDTO.OrderAddressSectionDTO section
    ) {
        order.setShippingName(trimToNull(section.getName()));
        order.setShippingPhone(trimToNull(section.getPhone()));
        order.setShippingEmail(trimToNull(section.getEmail()));
        order.setShippingAddress1(trimToNull(section.getAddressLine1()));
        order.setShippingAddress2(trimToNull(section.getAddressLine2()));
        order.setShippingCity(trimToNull(section.getCity()));
        order.setShippingState(trimToNull(section.getState()));
        order.setShippingPincode(trimToNull(section.getPincode()));
        order.setShippingCountry(
                section.getCountry() != null && !section.getCountry().isBlank()
                        ? section.getCountry().trim()
                        : "India"
        );
    }

    private void applyBillingSection(
            Order order,
            UpdateOrderAddressRequestDTO.OrderAddressSectionDTO section
    ) {
        order.setBillingName(trimToNull(section.getName()));
        order.setBillingPhone(trimToNull(section.getPhone()));
        order.setBillingEmail(trimToNull(section.getEmail()));
        order.setBillingAddress1(trimToNull(section.getAddressLine1()));
        order.setBillingAddress2(trimToNull(section.getAddressLine2()));
        order.setBillingCity(trimToNull(section.getCity()));
        order.setBillingState(trimToNull(section.getState()));
        order.setBillingPincode(trimToNull(section.getPincode()));
        order.setBillingCountry(
                section.getCountry() != null && !section.getCountry().isBlank()
                        ? section.getCountry().trim()
                        : "India"
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void notifySellersOfAddressUpdate(Order order, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> sellerIds = new LinkedHashSet<>();
        for (OrderItem item : items) {
            if (item.getSellerId() != null && item.getSellerId() > 0) {
                sellerIds.add(item.getSellerId());
            }
        }

        String orderLabel = order.getOrderNumber() != null ? order.getOrderNumber() : String.valueOf(order.getId());
        String shipSummary = formatShippingAddress(order).replace("\n", ", ");
        String title = "Order address updated";
        String message = "Customer updated the delivery/billing address for order #"
                + orderLabel
                + ". New shipping address: "
                + (shipSummary.isBlank() ? "Updated" : shipSummary);

        for (Long sellerId : sellerIds) {
            try {
                entityManager.createNativeQuery(
                                "INSERT INTO seller_notifications (seller_id, title, message, is_read, created_at) "
                                        + "VALUES (?1, ?2, ?3, false, CURRENT_TIMESTAMP)"
                        )
                        .setParameter(1, sellerId)
                        .setParameter(2, title)
                        .setParameter(3, message)
                        .executeUpdate();
            } catch (Exception e) {
                log.warn(
                        "[NOTIFY] Failed seller_notifications insert sellerId={} order={}: {}",
                        sellerId,
                        orderLabel,
                        e.getMessage()
                );
            }

            try {
                Seller seller = sellerRepository.findById(sellerId).orElse(null);
                String sellerEmail = seller != null ? firstNonBlank(seller.getEmail()) : null;
                if (sellerEmail == null) {
                    continue;
                }
                String html = "<p>Hello,</p>"
                        + "<p>The customer updated the address for order <strong>#"
                        + escapeHtml(orderLabel)
                        + "</strong>.</p>"
                        + "<p><strong>Shipping address</strong><br/>"
                        + escapeHtml(shipSummary.isBlank() ? "Updated" : shipSummary)
                        + "</p>"
                        + "<p>Please use the latest address when packing and shipping this order.</p>"
                        + "<p>— Flint &amp; Thread</p>";
                emailService.sendNoticeEmail(
                        sellerEmail,
                        "Address updated — Order #" + orderLabel,
                        html
                );
            } catch (Exception e) {
                log.warn(
                        "[EMAIL] Address-update notice failed sellerId={} order={}: {}",
                        sellerId,
                        orderLabel,
                        e.getMessage()
                );
            }
        }
    }

    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}