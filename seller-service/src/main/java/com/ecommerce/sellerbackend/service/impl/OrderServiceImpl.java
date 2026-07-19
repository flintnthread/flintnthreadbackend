package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.order.OrderEmailLogDto;
import com.ecommerce.sellerbackend.dto.order.OrderExchangeDto;
import com.ecommerce.sellerbackend.dto.order.OrderGstDto;
import com.ecommerce.sellerbackend.dto.order.OrderItemCancellationDto;
import com.ecommerce.sellerbackend.dto.order.OrderItemCustomDetailDto;
import com.ecommerce.sellerbackend.dto.order.OrderReplacementDto;
import com.ecommerce.sellerbackend.dto.order.OrderReturnDto;
import com.ecommerce.sellerbackend.dto.order.OrderReviewSummaryDto;
import com.ecommerce.sellerbackend.dto.order.OrderStatusHistoryEntryDto;
import com.ecommerce.sellerbackend.dto.order.SellerCustomerDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderDetailDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderLineDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderStepDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderStatsDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderSummaryDto;
import com.ecommerce.sellerbackend.dto.order.SellerPaymentDto;
import com.ecommerce.sellerbackend.dto.order.SellerPricingDto;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.entity.OrderEmailLog;
import com.ecommerce.sellerbackend.entity.OrderExchange;
import com.ecommerce.sellerbackend.entity.OrderGst;
import com.ecommerce.sellerbackend.entity.OrderItem;
import com.ecommerce.sellerbackend.entity.OrderItemCancellation;
import com.ecommerce.sellerbackend.entity.OrderItemCustomDetail;
import com.ecommerce.sellerbackend.entity.OrderReplacement;
import com.ecommerce.sellerbackend.entity.OrderReturn;
import com.ecommerce.sellerbackend.entity.OrderStatusHistory;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.repository.OrderEmailLogRepository;
import com.ecommerce.sellerbackend.repository.OrderExchangeRepository;
import com.ecommerce.sellerbackend.repository.OrderGstRepository;
import com.ecommerce.sellerbackend.repository.OrderItemCancellationRepository;
import com.ecommerce.sellerbackend.repository.OrderItemCustomDetailRepository;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.OrderReplacementRepository;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.repository.OrderReturnRepository;
import com.ecommerce.sellerbackend.repository.OrderStatusHistoryRepository;
import com.ecommerce.sellerbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter STEP_DATE =
            DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH);
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderEmailLogRepository orderEmailLogRepository;
    private final OrderGstRepository orderGstRepository;
    private final OrderItemCancellationRepository orderItemCancellationRepository;
    private final OrderItemCustomDetailRepository orderItemCustomDetailRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderReplacementRepository orderReplacementRepository;
    private final OrderExchangeRepository orderExchangeRepository;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Override
    public List<SellerOrderSummaryDto> listForSeller(Long sellerId) {
        List<OrderItem> allItems = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        LineCatalog catalog = loadLineCatalog(allItems);
        Map<Integer, String> resolvedNamesByItemId = loadResolvedNamesByItemIds(allItems);
        Map<Long, List<OrderItem>> grouped = groupItemsByOrder(allItems);
        Map<Long, Order> orders = loadOrders(grouped.keySet());

        return grouped.entrySet().stream()
                .sorted(Comparator.comparing(
                        (Map.Entry<Long, List<OrderItem>> e) -> latestItemTime(e.getValue()),
                        Comparator.reverseOrder()))
                .map(entry -> toSummary(
                        entry.getKey(),
                        entry.getValue(),
                        orders.get(entry.getKey()),
                        catalog,
                        resolvedNamesByItemId))
                .toList();
    }

    @Override
    public List<SellerOrderDetailDto> listDetailsForSeller(Long sellerId) {
        List<OrderItem> allItems = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        Map<Integer, String> resolvedNamesByItemId = loadResolvedNamesByItemIds(allItems);
        LineCatalog catalog = loadLineCatalog(allItems);
        Map<Long, List<OrderItem>> grouped = groupItemsByOrder(allItems);
        Map<Long, Order> orders = loadOrders(grouped.keySet());

        return grouped.entrySet().stream()
                .sorted(Comparator.comparing(
                        (Map.Entry<Long, List<OrderItem>> e) -> latestItemTime(e.getValue()),
                        Comparator.reverseOrder()))
                .map(entry -> toDetail(
                        resolveOrder(entry.getKey(), entry.getValue()),
                        entry.getValue(),
                        resolvedNamesByItemId,
                        catalog))
                .toList();
    }

    @Override
    public SellerOrderStatsDto statsForSeller(Long sellerId) {
        List<OrderItem> items = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        java.util.Set<Long> orderIds = items.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Order> orders = loadOrders(orderIds);
        Map<Long, List<OrderStatusHistory>> historyByOrder = new HashMap<>();
        for (Long orderId : orderIds) {
            historyByOrder.put(orderId, loadStatusHistory(orderId));
        }

        int pending = 0;
        int processing = 0;
        int shipped = 0;
        int delivered = 0;
        int returns = 0;
        int cancelled = 0;
        BigDecimal totalSale = BigDecimal.ZERO;

        for (OrderItem item : items) {
            Order order = orders.get(item.getOrderId());
            List<OrderItem> orderItems = List.of(item);
            if (order == null) {
                order = buildSyntheticOrder(item.getOrderId(), List.of(item));
            }
            List<OrderStatusHistory> history = historyByOrder.getOrDefault(item.getOrderId(), List.of());
            String rawStatus;
            if (order != null && isCancelledStatus(order.getOrderStatus())) {
                rawStatus = "cancelled";
            } else if (item.getStatus() != null && !item.getStatus().isBlank()) {
                rawStatus = item.getStatus();
            } else {
                rawStatus = resolveRawStatus(order, orderItems, history);
            }
            String uiStatus = toUiStatus(rawStatus);
            switch (uiStatus) {
                case "Pending" -> pending++;
                case "Processing" -> processing++;
                case "Shipped" -> shipped++;
                case "Delivered" -> delivered++;
                case "Returned" -> returns++;
                case "Cancelled" -> cancelled++;
                default -> pending++;
            }
            totalSale = totalSale.add(lineAmount(item));
        }

        return SellerOrderStatsDto.builder()
                .totalLineItems(items.size())
                .totalOrders(orderIds.size())
                .allItems(items.size())
                .pending(pending)
                .processing(processing)
                .shipped(shipped)
                .delivered(delivered)
                .returns(returns)
                .cancelled(cancelled)
                .totalSale(totalSale)
                .build();
    }

    private Order resolveOrder(Long orderId, List<OrderItem> items) {
        return orderRepository.findById(orderId)
                .orElseGet(() -> buildSyntheticOrder(orderId, items));
    }

    /** When the orders row is missing, build a minimal header from this seller's line items. */
    private Order buildSyntheticOrder(Long orderId, List<OrderItem> items) {
        Order order = new Order();
        order.setId(orderId);
        OrderItem first = items.get(0);
        order.setCreatedAt(first.getCreatedAt());
        order.setUpdatedAt(first.getCreatedAt());
        order.setOrderStatus(first.getStatus());
        order.setShippingAmount(BigDecimal.ZERO);
        return order;
    }

    @Override
    public SellerOrderDetailDto getForSeller(Long sellerId, String orderKey) {
        Long orderId = resolveOrderId(sellerId, orderKey);
        List<OrderItem> items = orderItemRepository.findBySellerIdAndOrderId(sellerId, orderId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Order not found for this seller.");
        }
        Order order = resolveOrder(orderId, items);
        return toDetail(order, items);
    }

    @Override
    @Transactional
    public SellerOrderDetailDto updateStatusForSeller(
            Long sellerId, String orderKey, String status, String comment) {
        String dbStatus = toDbStatus(status);
        String historyStatus = toHistoryStatus(status);
        Long orderId = resolveOrderId(sellerId, orderKey);
        List<OrderItem> items = orderItemRepository.findBySellerIdAndOrderId(sellerId, orderId);
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Order not found for this seller.");
        }

        LocalDateTime now = LocalDateTime.now();
        for (OrderItem item : items) {
            item.setStatus(dbStatus);
        }
        orderItemRepository.saveAll(items);

        Order order = resolveOrder(orderId, items);
        order.setOrderStatus(dbStatus);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        recordStatusHistory(orderId, historyStatus, comment, sellerId, now);

        return toDetail(order, items);
    }

    private void recordStatusHistory(
            Long orderId, String historyStatus, String comment, Long createdBy, LocalDateTime at) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrderId(orderId);
        entry.setStatus(historyStatus);
        entry.setComment(comment != null && !comment.isBlank() ? comment.trim() : null);
        entry.setCreatedBy(createdBy);
        entry.setCreatedAt(at);
        orderStatusHistoryRepository.save(entry);
    }

    private List<OrderStatusHistory> loadStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    private Map<Long, List<OrderItem>> groupItemsByOrder(List<OrderItem> items) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, Order> loadOrders(java.util.Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderRepository.findByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));
    }

    private Long resolveOrderId(Long sellerId, String orderKey) {
        if (orderKey == null || orderKey.isBlank()) {
            throw new IllegalArgumentException("Order id is required.");
        }
        String key = orderKey.trim();
        if (key.startsWith("#")) {
            key = key.substring(1);
        }

        if (key.chars().allMatch(Character::isDigit)) {
            Long numericId = Long.parseLong(key);
            List<OrderItem> byNumeric = orderItemRepository.findBySellerIdAndOrderId(sellerId, numericId);
            if (!byNumeric.isEmpty()) {
                return numericId;
            }
        }

        return orderRepository.findByOrderNumber(key)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findBySellerIdAndOrderId(sellerId, order.getId());
                    if (items.isEmpty()) {
                        throw new ResourceNotFoundException("Order not found for this seller.");
                    }
                    return order.getId();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for this seller."));
    }

    private SellerOrderSummaryDto toSummary(
            Long orderId,
            List<OrderItem> items,
            Order order,
            LineCatalog catalog,
            Map<Integer, String> resolvedNamesByItemId) {
        Order resolved = order != null ? order : buildSyntheticOrder(orderId, items);
        OrderItem first = items.get(0);
        int totalQty = items.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        SellerItemTotals totals = computeSellerItemTotals(items);
        String displayId = displayOrderId(resolved, orderId);
        List<OrderStatusHistory> statusHistory = loadStatusHistory(orderId);
        String uiStatus = toUiStatus(resolveRawStatus(resolved, items, statusHistory));
        Map<Long, String> productNames = loadProductNames(items);
        String productLabel = buildProductLabel(items, productNames, resolvedNamesByItemId);
        String color = resolveLineColor(first, catalog);
        String size = resolveLineSize(first, catalog);

        return SellerOrderSummaryDto.builder()
                .id(displayId)
                .orderId(orderId)
                .date(formatDateTime(resolved.getCreatedAt() != null ? resolved.getCreatedAt() : first.getCreatedAt()))
                .product(productLabel)
                .variant(buildVariantLabel(color, size))
                .qty(totalQty)
                .price(formatInr(totals.total()))
                .priceAmount(totals.total())
                .subtotalAmount(totals.subtotal())
                .taxAmount(totals.tax())
                .discountAmount(totals.discount())
                .itemCount(items.size())
                .status(uiStatus)
                .customer(resolved.getShippingName() != null && !resolved.getShippingName().isBlank()
                        ? resolved.getShippingName()
                        : "Customer")
                .image(resolveLineImage(first, catalog))
                .extra(buildExtraNote(resolved))
                .build();
    }

    private String buildProductLabel(
            List<OrderItem> items,
            Map<Long, String> productNames,
            Map<Integer, String> resolvedNamesByItemId) {
        OrderItem first = items.get(0);
        String name = resolveLineName(first, productNames, resolvedNamesByItemId);
        if (items.size() <= 1) {
            return name;
        }
        return name + " (+" + (items.size() - 1) + " more item" + (items.size() > 2 ? "s" : "") + ")";
    }

    private SellerOrderDetailDto toDetail(Order order, List<OrderItem> items) {
        return toDetail(order, items, loadResolvedNamesByItemIds(items), loadLineCatalog(items));
    }

    private SellerOrderDetailDto toDetail(
            Order order,
            List<OrderItem> items,
            Map<Integer, String> resolvedNamesByItemId,
            LineCatalog catalog) {
        items = items.stream()
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Integer> itemIds = items.stream()
                .map(OrderItem::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, OrderItem> itemById = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(OrderItem::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<OrderStatusHistory> statusHistory = loadStatusHistory(order.getId());
        String rawStatus = resolveRawStatus(order, items, statusHistory);
        String uiStatus = toUiStatus(rawStatus);
        LocalDateTime createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : items.get(0).getCreatedAt();

        Map<Integer, List<OrderItemCustomDetail>> customByItem = loadCustomDetailsByItem(itemIds);
        Map<Long, String> productNames = loadProductNames(items);
        List<SellerOrderLineDto> lineDtos = items.stream()
                .map(item -> toLineDto(
                        item,
                        customByItem.getOrDefault(item.getId(), List.of()),
                        productNames,
                        resolvedNamesByItemId,
                        rawStatus,
                        catalog))
                .toList();

        List<OrderEmailLogDto> emailLogs = loadEmailLogs(order.getId());
        List<OrderGstDto> gstRecords = loadGstRecords(order);
        List<OrderReturnDto> returns = toReturnDtos(
                itemIds.isEmpty() ? List.of() : orderReturnRepository.findByOrderItemIdInOrderByCreatedAtDesc(itemIds),
                itemById);
        List<OrderExchangeDto> exchanges = toExchangeDtos(
                itemIds.isEmpty() ? List.of() : orderExchangeRepository.findByOrderItemIdInOrderByCreatedAtDesc(itemIds),
                itemById);
        List<OrderReplacementDto> replacements = toReplacementDtos(
                itemIds.isEmpty() ? List.of() : orderReplacementRepository.findByOrderItemIdInOrderByCreatedAtDesc(itemIds),
                itemById);
        List<OrderItemCancellationDto> cancellations = toCancellationDtos(
                itemIds.isEmpty() ? List.of() : orderItemCancellationRepository.findByOrderItemIdInOrderByCreatedAtDesc(itemIds),
                itemById);
        OrderReviewSummaryDto reviewSummary = buildReviewSummary(returns, exchanges, replacements, cancellations);

        Map<String, String> stepDates = buildStepDates(order, items, uiStatus, statusHistory);
        List<OrderStatusHistoryEntryDto> historyDtos = statusHistory.stream()
                .map(this::toHistoryDto)
                .toList();

        String gstNumber = resolveGstNumber(order, gstRecords);
        String gstInfo = resolveGstInfo(order, gstRecords);

        return SellerOrderDetailDto.builder()
                .id(displayOrderId(order, order.getId()))
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .date(formatDateTime(createdAt))
                .status(uiStatus)
                .dbStatus(rawStatus)
                .customer(buildCustomer(order))
                .billing(buildBilling(order))
                .items(lineDtos)
                .pricing(buildPricing(order, items))
                .payment(buildPayment(order, createdAt, uiStatus))
                .steps(buildSteps(uiStatus, stepDates, statusHistory))
                .statusHistory(historyDtos)
                .emailLogs(emailLogs)
                .gstRecords(gstRecords)
                .returns(returns)
                .exchanges(exchanges)
                .replacements(replacements)
                .cancellations(cancellations)
                .reviewSummary(reviewSummary)
                .customerNote(order.getOrderNotes())
                .sellerNote(order.getOrderNotes())
                .cancelReason(null)
                .gstNumber(gstNumber)
                .gstInfo(gstInfo)
                .primaryActionLabel(primaryActionLabel(uiStatus, reviewSummary))
                .secondaryActionLabel(secondaryActionLabel(uiStatus, reviewSummary))
                .extraNote(buildExtraNote(order))
                .shiprocketOrderId(order.getShiprocketOrderId())
                .shiprocketShipmentId(order.getShiprocketShipmentId())
                .shiprocketAwbCode(order.getShiprocketAwbCode())
                .shiprocketCourierName(order.getShiprocketCourierName())
                .shiprocketStatus(order.getShiprocketStatus())
                .shiprocketTrackingUrl(order.getShiprocketTrackingUrl())
                .shiprocketSyncedAt(order.getShiprocketSyncedAt() != null
                        ? formatDateTime(order.getShiprocketSyncedAt())
                        : null)
                .build();
    }

    private SellerPricingDto buildPricing(Order order, List<OrderItem> items) {
        SellerItemTotals totals = computeSellerItemTotals(items);

        return SellerPricingDto.builder()
                .subtotal(formatInr(totals.subtotal()))
                .shipping(formatInr(BigDecimal.ZERO))
                .tax(totals.tax().compareTo(BigDecimal.ZERO) > 0 ? formatInr(totals.tax()) : null)
                .discount(totals.discount().compareTo(BigDecimal.ZERO) > 0 ? formatInr(totals.discount().negate()) : null)
                .referralDiscount(null)
                .walletDeduction(null)
                .total(formatInr(totals.total()))
                .subtotalAmount(totals.subtotal())
                .taxAmount(totals.tax())
                .discountAmount(totals.discount())
                .totalAmount(totals.total())
                .build();
    }

    private BigDecimal resolveOrderTotal(Order order, List<OrderItem> items) {
        return computeSellerItemTotals(items).total();
    }

    private record SellerItemTotals(
            BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total) {
    }

    /** Totals for this seller's line items only (from order_items columns). */
    private SellerItemTotals computeSellerItemTotals(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            subtotal = subtotal.add(lineSubtotal(item));
            total = total.add(lineAmount(item));
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            total = subtotal.max(BigDecimal.ZERO);
        }

        return new SellerItemTotals(subtotal, tax, discount, total);
    }

    private BigDecimal lineSubtotal(OrderItem item) {
        BigDecimal unit = resolveUnitPrice(item);
        int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveUnitPrice(OrderItem item) {
        if (item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return item.getPrice();
        }
        if (item.getTotal() != null
                && item.getTotal().compareTo(BigDecimal.ZERO) > 0
                && item.getQuantity() != null
                && item.getQuantity() > 0) {
            return item.getTotal()
                    .divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal lineAmount(OrderItem item) {
        if (item.getTotal() != null && item.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            return item.getTotal();
        }
        return lineSubtotal(item);
    }

    private BigDecimal nullSafeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private SellerCustomerDto buildBilling(Order order) {
        if (order.getBillingName() == null && order.getBillingAddress1() == null) {
            return null;
        }
        String cityLine = java.util.stream.Stream.of(order.getBillingCity(), order.getBillingState())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        String pincode = nullSafe(order.getBillingPincode()).trim();
        if (!pincode.isBlank()) {
            cityLine = cityLine.isBlank() ? pincode : cityLine + " - " + pincode;
        }
        String address = joinNonBlank(
                order.getBillingAddress1(),
                order.getBillingAddress2(),
                cityLine,
                order.getBillingCountry());

        return SellerCustomerDto.builder()
                .name(nullToDefault(order.getBillingName(), ""))
                .phone(nullToDefault(order.getBillingPhone(), ""))
                .email(nullToDefault(order.getBillingEmail(), ""))
                .address(address)
                .build();
    }

    private Map<Long, String> loadProductNames(List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (Object[] row : productRepository.findNamesByProductIds(productIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            String name = String.valueOf(row[1]).trim();
            if (!isGenericProductLabel(name)) {
                names.putIfAbsent(id, name);
            }
        }
        if (names.size() < productIds.size()) {
            productRepository.findAllById(productIds).forEach(product -> {
                String name = product.getName() != null ? product.getName().trim() : "";
                if (!isGenericProductLabel(name)) {
                    names.putIfAbsent(product.getId(), name);
                }
            });
        }
        return names;
    }

    private Map<Integer, String> loadResolvedNamesByItemIds(List<OrderItem> items) {
        List<Integer> itemIds = items.stream()
                .map(OrderItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> names = new LinkedHashMap<>();
        for (Object[] row : orderItemRepository.findResolvedProductNamesByItemIds(itemIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            names.put(((Number) row[0]).intValue(), String.valueOf(row[1]).trim());
        }
        return names;
    }

    private static boolean isGenericProductLabel(String name) {
        return name == null || name.isBlank() || "product".equalsIgnoreCase(name.trim());
    }

    private String resolveProductName(OrderItem item, Map<Long, String> productNames) {
        String fromCatalog = null;
        if (item.getProductId() != null) {
            String catalog = productNames.get(item.getProductId());
            if (!isGenericProductLabel(catalog)) {
                fromCatalog = catalog.trim();
            }
        }

        String stored = item.getProductName() != null ? item.getProductName().trim() : "";
        if (!isGenericProductLabel(stored)) {
            return stored;
        }

        if (fromCatalog != null) {
            return fromCatalog;
        }

        if (!stored.isBlank()) {
            return stored;
        }

        if (item.getSku() != null && !item.getSku().isBlank()) {
            return item.getSku().trim();
        }

        return "Product";
    }

    private String resolveLineName(
            OrderItem item,
            Map<Long, String> productNames,
            Map<Integer, String> resolvedNamesByItemId) {
        if (item.getId() != null) {
            String resolved = resolvedNamesByItemId.get(item.getId());
            if (!isGenericProductLabel(resolved)) {
                return resolved.trim();
            }
        }
        return resolveProductName(item, productNames);
    }

    private SellerOrderLineDto toLineDto(
            OrderItem item,
            List<OrderItemCustomDetail> customDetails,
            Map<Long, String> productNames,
            Map<Integer, String> resolvedNamesByItemId,
            String orderRawStatus,
            LineCatalog catalog) {
        BigDecimal amount = lineAmount(item);
        BigDecimal subtotal = lineSubtotal(item);
        String effectiveStatus = isCancelledStatus(orderRawStatus)
                ? "cancelled"
                : (item.getStatus() != null && !item.getStatus().isBlank()
                        ? item.getStatus()
                        : orderRawStatus);
        String color = resolveLineColor(item, catalog);
        String size = resolveLineSize(item, catalog);
        String sku = resolveLineSku(item, catalog);
        String hsn = resolveLineHsn(item, catalog);
        return SellerOrderLineDto.builder()
                .lineItemId(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .sellerId(item.getSellerId())
                .name(resolveLineName(item, productNames, resolvedNamesByItemId))
                .variant(buildVariantLabel(color, size))
                .sku(sku)
                .qty(item.getQuantity() != null ? item.getQuantity() : 0)
                .price(formatInr(amount))
                .priceAmount(amount)
                .subtotalAmount(subtotal)
                .image(resolveLineImage(item, catalog))
                .hsnCode(hsn)
                .weight(item.getWeight())
                .lengthCm(item.getLengthCm())
                .widthCm(item.getWidthCm())
                .heightCm(item.getHeightCm())
                .packageDeadWeight(item.getPackageDeadWeight())
                .volumetricWeight(item.getVolumetricWeight())
                .chargeableWeight(item.getChargeableWeight())
                .unitPrice(resolveUnitPrice(item))
                .discount(null)
                .tax(null)
                .status(effectiveStatus)
                .uiStatus(toUiStatus(effectiveStatus))
                .color(color)
                .size(size)
                .sellerName(item.getSellerName())
                .customDetails(customDetails.stream().map(this::toCustomDetailDto).toList())
                .build();
    }

    private SellerCustomerDto buildCustomer(Order order) {
        String cityLine = java.util.stream.Stream.of(order.getShippingCity(), order.getShippingState())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        String pincode = nullSafe(order.getShippingPincode()).trim();
        if (!pincode.isBlank()) {
            cityLine = cityLine.isBlank() ? pincode : cityLine + " - " + pincode;
        }

        String address = joinNonBlank(
                order.getShippingAddress1(),
                order.getShippingAddress2(),
                cityLine,
                order.getShippingCountry());

        return SellerCustomerDto.builder()
                .name(nullToDefault(order.getShippingName(), "Customer"))
                .phone(nullToDefault(order.getShippingPhone(), ""))
                .email(nullToDefault(order.getShippingEmail(), ""))
                .address(address)
                .build();
    }

    private SellerPaymentDto buildPayment(Order order, LocalDateTime createdAt, String uiStatus) {
        String method = formatPaymentMethod(order.getPaymentMethod());
        boolean cod = isCodPaymentMethod(order.getPaymentMethod());
        boolean collected = !cod || "Delivered".equals(uiStatus) || "Returned".equals(uiStatus);

        String status;
        boolean paymentCompleted;
        if (cod) {
            status = collected ? "Paid" : "Pending";
            paymentCompleted = collected;
        } else {
            status = formatPaymentStatus(order.getPaymentStatus());
            if (isPaymentCompleted(order)) {
                status = "Paid";
            }
            paymentCompleted = isPaymentCompleted(order);
        }

        String txnId = order.getRazorpayPaymentId() != null
                ? order.getRazorpayPaymentId()
                : order.getRazorpayOrderId() != null ? order.getRazorpayOrderId() : "";

        return SellerPaymentDto.builder()
                .method(method)
                .status(status)
                .sellerPaymentStatus(formatSellerPaymentStatus(order.getSellerPaymentStatus()))
                .paymentCompleted(paymentCompleted)
                .transactionId(txnId)
                .paidOn(collected ? formatDateTime(createdAt) : "")
                .bankOrUpiId(method)
                .refNo(txnId)
                .razorpayOrderId(order.getRazorpayOrderId())
                .build();
    }

    private boolean isCodPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return false;
        }
        String pm = paymentMethod.trim().toLowerCase(Locale.ROOT);
        return pm.contains("cod") || pm.contains("cash") || "cash_on_delivery".equals(pm);
    }

    private String formatSellerPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "paid" -> "Paid";
            case "cancelled", "canceled" -> "Cancelled";
            default -> capitalize(status);
        };
    }

    private boolean isPaymentCompleted(Order order) {
        if (order == null || order.getPaymentStatus() == null) {
            return false;
        }
        String pay = order.getPaymentStatus().trim().toLowerCase(Locale.ROOT);
        return pay.contains("paid") || pay.contains("success") || pay.contains("completed");
    }

    private OrderStatusHistoryEntryDto toHistoryDto(OrderStatusHistory entry) {
        return OrderStatusHistoryEntryDto.builder()
                .id(entry.getId())
                .orderId(entry.getOrderId())
                .status(entry.getStatus())
                .statusLabel(formatHistoryStatusLabel(entry.getStatus()))
                .comment(entry.getComment())
                .createdBy(entry.getCreatedBy())
                .createdAt(entry.getCreatedAt() != null ? formatDateTime(entry.getCreatedAt()) : "")
                .build();
    }

    private String formatHistoryStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pending" -> "Pending";
            case "sent_to_seller" -> "Sent to Seller";
            case "processing" -> "Processing";
            case "completed" -> "Completed";
            case "cancelled" -> "Cancelled";
            case "refunded" -> "Refunded";
            case "returned" -> "Returned";
            case "replacement" -> "Replacement";
            case "awaiting_processing" -> "Awaiting Processing";
            case "awaiting_payment" -> "Awaiting Payment";
            default -> capitalize(status.replace('_', ' '));
        };
    }

    private List<SellerOrderStepDto> buildSteps(
            String uiStatus, Map<String, String> dates, List<OrderStatusHistory> history) {
        List<StepDef> flow = new ArrayList<>(List.of(
                new StepDef("pending", "Pending", "MCIcons", "clipboard-text-outline"),
                new StepDef("processing", "Processing", "MCIcons", "package-variant-closed"),
                new StepDef("shipped", "Shipped", "MCIcons", "truck-outline"),
                new StepDef("delivered", "Delivered", "Ionicons", "checkmark-circle-outline")
        ));

        if ("Returned".equals(uiStatus)) {
            flow.set(3, new StepDef("returned", "Returned", "MCIcons", "arrow-u-left-top"));
        } else if ("Cancelled".equals(uiStatus)) {
            flow.set(3, new StepDef("cancelled", "Cancelled", "Ionicons", "close-circle-outline"));
        }

        int activeIndex = switch (uiStatus) {
            case "Pending" -> 0;
            case "Processing" -> 1;
            case "Shipped" -> 2;
            case "Delivered", "Returned", "Cancelled" -> 3;
            default -> 0;
        };

        List<SellerOrderStepDto> steps = new ArrayList<>();
        for (int i = 0; i < flow.size(); i++) {
            StepDef step = flow.get(i);
            String stepStatus = i < activeIndex ? "done" : i == activeIndex ? "active" : "pending";
            String stepComment = findStepComment(history, step.key());
            steps.add(SellerOrderStepDto.builder()
                    .key(step.key())
                    .label(step.label())
                    .date(dates.get(step.key()))
                    .iconLib(step.iconLib())
                    .iconName(step.iconName())
                    .status(stepStatus)
                    .comment(stepComment)
                    .build());
        }
        return steps;
    }

    private String findStepComment(List<OrderStatusHistory> history, String stepKey) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            OrderStatusHistory entry = history.get(i);
            String mappedKey = historyStatusToStepKey(entry.getStatus());
            if (stepKey.equals(mappedKey) && entry.getComment() != null && !entry.getComment().isBlank()) {
                return entry.getComment();
            }
        }
        return null;
    }

    private Map<String, String> buildStepDates(
            Order order, List<OrderItem> items, String uiStatus, List<OrderStatusHistory> history) {
        Map<String, String> dates = new LinkedHashMap<>();

        if (history != null && !history.isEmpty()) {
            for (OrderStatusHistory entry : history) {
                String stepKey = historyStatusToStepKey(entry.getStatus());
                if (stepKey != null && entry.getCreatedAt() != null) {
                    dates.put(stepKey, formatStepDate(entry.getCreatedAt()));
                }
            }
            if (!dates.isEmpty()) {
                return dates;
            }
        }

        LocalDateTime created = order.getCreatedAt() != null ? order.getCreatedAt() : items.get(0).getCreatedAt();
        if (created != null) {
            dates.put("pending", formatStepDate(created));
        }
        if (order.getUpdatedAt() != null) {
            String formatted = formatStepDate(order.getUpdatedAt());
            switch (uiStatus) {
                case "Processing" -> dates.put("processing", formatted);
                case "Shipped" -> {
                    dates.put("processing", formatted);
                    dates.put("shipped", formatted);
                }
                case "Delivered", "Returned", "Cancelled" -> {
                    dates.put("processing", formatted);
                    dates.put("shipped", formatted);
                    dates.put(uiStatus.equals("Delivered") ? "delivered"
                            : uiStatus.equals("Returned") ? "returned" : "cancelled", formatted);
                }
                default -> { }
            }
        }
        return dates;
    }

    private String historyStatusToStepKey(String historyStatus) {
        if (historyStatus == null || historyStatus.isBlank()) {
            return null;
        }
        return switch (historyStatus.trim().toLowerCase(Locale.ROOT)) {
            case "pending", "sent_to_seller", "awaiting_payment", "awaiting_processing" -> "pending";
            case "processing" -> "processing";
            case "completed" -> "delivered";
            case "cancelled" -> "cancelled";
            case "returned", "refunded", "replacement" -> "returned";
            default -> null;
        };
    }

    private String toHistoryStatus(String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank()) {
            return "pending";
        }
        return switch (uiStatus.trim()) {
            case "Pending" -> "pending";
            case "Processing" -> "processing";
            case "Shipped" -> "processing";
            case "Delivered" -> "completed";
            case "Returned" -> "returned";
            case "Cancelled" -> "cancelled";
            default -> uiStatus.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String resolveRawStatus(Order order, List<OrderItem> items, List<OrderStatusHistory> history) {
        // Terminal cancel on the order header wins over stale history / line status
        // (user cancel historically only updated orders.order_status).
        if (order != null && isCancelledStatus(order.getOrderStatus())) {
            return "cancelled";
        }
        if (history != null && !history.isEmpty()) {
            OrderStatusHistory latest = history.get(history.size() - 1);
            if (latest.getStatus() != null && !latest.getStatus().isBlank()) {
                return latest.getStatus().trim();
            }
        }
        if (order != null && order.getOrderStatus() != null && !order.getOrderStatus().isBlank()) {
            return order.getOrderStatus().trim();
        }
        return items.stream()
                .map(OrderItem::getStatus)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("pending");
    }

    private boolean isCancelledStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return "cancelled".equals(normalized)
                || "canceled".equals(normalized)
                || normalized.contains("cancel");
    }

    private String toUiStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Pending";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "pending", "sent_to_seller", "awaiting_payment", "awaiting_processing",
                    "new", "placed" -> "Pending";
            case "confirmed", "processing", "packed", "awb_assigned", "pickup_scheduled", "accepted" -> "Processing";
            case "picked_up", "in_transit", "out_for_delivery", "shipped", "ready_to_ship" -> "Shipped";
            case "delivered", "completed" -> "Delivered";
            case "returned", "return", "refunded", "rto_initiated", "rto_delivered", "replacement" -> "Returned";
            case "cancelled", "canceled", "rejected" -> "Cancelled";
            default -> capitalize(raw.replace('_', ' '));
        };
    }

    private String toDbStatus(String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        return switch (uiStatus.trim()) {
            case "Pending" -> "confirmed";
            case "Processing" -> "processing";
            case "Shipped" -> "in_transit";
            case "Delivered" -> "delivered";
            case "Returned" -> "returned";
            case "Cancelled" -> "cancelled";
            default -> uiStatus.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String displayOrderId(Order order, Long orderId) {
        if (order != null && order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {
            String num = order.getOrderNumber().trim();
            return num.startsWith("#") ? num : "#" + num;
        }
        return "#ORD-" + orderId;
    }

    private String buildVariant(OrderItem item) {
        return buildVariantLabel(item.getColor(), item.getSize());
    }

    private String buildVariantLabel(String color, String size) {
        boolean hasColor = color != null && !color.isBlank() && !"—".equals(color.trim());
        boolean hasSize = size != null && !size.isBlank() && !"—".equals(size.trim());
        if (hasColor && hasSize) {
            return color.trim() + " • " + size.trim();
        }
        if (hasColor) {
            return color.trim();
        }
        if (hasSize) {
            return size.trim();
        }
        return "";
    }

    private record LineCatalog(
            Map<Long, ProductVariant> variantsById,
            Map<Long, Product> productsById,
            Map<Long, List<ProductImage>> imagesByProductId,
            Map<Long, Color> colorById,
            Map<Long, Size> sizeById
    ) {
        static LineCatalog empty() {
            return new LineCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    private LineCatalog loadLineCatalog(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return LineCatalog.empty();
        }

        Set<Long> variantIds = items.stream()
                .map(OrderItem::getVariantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, ProductVariant> variantsById = new HashMap<>();
        if (!variantIds.isEmpty()) {
            for (ProductVariant variant : productVariantRepository.findAllById(variantIds)) {
                variantsById.put(variant.getId(), variant);
            }
        }

        Set<Long> productIds = new LinkedHashSet<>();
        for (OrderItem item : items) {
            if (item.getProductId() != null) {
                productIds.add(item.getProductId());
            } else if (item.getVariantId() != null) {
                ProductVariant variant = variantsById.get(item.getVariantId());
                if (variant != null && variant.getProductId() != null) {
                    productIds.add(variant.getProductId());
                }
            }
        }

        Map<Long, Product> productsById = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product product : productRepository.findAllById(productIds)) {
                productsById.put(product.getId(), product);
            }
        }

        Map<Long, List<ProductImage>> imagesByProductId = productIds.isEmpty()
                ? Map.of()
                : productImageRepository.findByProductIdInOrderByIsPrimaryDescSortOrderAsc(productIds).stream()
                        .collect(Collectors.groupingBy(
                                ProductImage::getProductId,
                                LinkedHashMap::new,
                                Collectors.toList()));

        Set<Long> colorIds = new HashSet<>();
        Set<Long> sizeIds = new HashSet<>();
        for (OrderItem item : items) {
            collectCatalogId(item.getColor(), colorIds);
            collectCatalogId(item.getSize(), sizeIds);
            ProductVariant variant = item.getVariantId() != null ? variantsById.get(item.getVariantId()) : null;
            if (variant != null) {
                collectCatalogId(variant.getColor(), colorIds);
                collectCatalogId(variant.getSize(), sizeIds);
            }
        }

        Map<Long, Color> colorById = new HashMap<>();
        if (!colorIds.isEmpty()) {
            for (Color color : colorRepository.findAllById(colorIds)) {
                colorById.put(color.getId(), color);
            }
        }
        Map<Long, Size> sizeById = new HashMap<>();
        if (!sizeIds.isEmpty()) {
            for (Size size : sizeRepository.findAllById(sizeIds)) {
                sizeById.put(size.getId(), size);
            }
        }

        return new LineCatalog(variantsById, productsById, imagesByProductId, colorById, sizeById);
    }

    private void collectCatalogId(String raw, Set<Long> ids) {
        parseCatalogId(raw).ifPresent(ids::add);
    }

    private Optional<Long> parseCatalogId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(raw.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String resolveLineColor(OrderItem item, LineCatalog catalog) {
        ProductVariant variant = item.getVariantId() != null
                ? catalog.variantsById().get(item.getVariantId())
                : null;
        String raw = firstNonBlank(item.getColor(), variant != null ? variant.getColor() : null);
        return resolveColorName(raw, catalog.colorById());
    }

    private String resolveLineSize(OrderItem item, LineCatalog catalog) {
        ProductVariant variant = item.getVariantId() != null
                ? catalog.variantsById().get(item.getVariantId())
                : null;
        String raw = firstNonBlank(item.getSize(), variant != null ? variant.getSize() : null);
        return resolveSizeName(raw, catalog.sizeById());
    }

    private String resolveColorName(String raw, Map<Long, Color> colorById) {
        return parseCatalogId(raw)
                .map(colorById::get)
                .map(Color::getColorName)
                .filter(name -> name != null && !name.isBlank())
                .orElseGet(() -> hasText(raw) ? raw.trim() : "");
    }

    private String resolveSizeName(String raw, Map<Long, Size> sizeById) {
        return parseCatalogId(raw)
                .map(sizeById::get)
                .map(Size::getSizeName)
                .filter(name -> name != null && !name.isBlank())
                .orElseGet(() -> hasText(raw) ? raw.trim() : "");
    }

    private String resolveLineSku(OrderItem item, LineCatalog catalog) {
        if (hasText(item.getSku())) {
            return item.getSku().trim();
        }
        ProductVariant variant = item.getVariantId() != null
                ? catalog.variantsById().get(item.getVariantId())
                : null;
        if (variant != null && hasText(variant.getSku())) {
            return variant.getSku().trim();
        }
        Long productId = resolveEffectiveProductId(item, catalog);
        Product product = productId != null ? catalog.productsById().get(productId) : null;
        if (product != null && hasText(product.getSku())) {
            return product.getSku().trim();
        }
        return "";
    }

    private String resolveLineHsn(OrderItem item, LineCatalog catalog) {
        if (hasText(item.getHsnCode())) {
            return item.getHsnCode().trim();
        }
        Long productId = resolveEffectiveProductId(item, catalog);
        Product product = productId != null ? catalog.productsById().get(productId) : null;
        if (product != null && hasText(product.getHsnCode())) {
            return product.getHsnCode().trim();
        }
        return item.getHsnCode();
    }

    private String resolveLineImage(OrderItem item, LineCatalog catalog) {
        if (hasText(item.getProductImagePath())) {
            return resolveImageUrl(item.getProductImagePath());
        }
        Long productId = resolveEffectiveProductId(item, catalog);
        if (productId == null) {
            return "";
        }
        List<ProductImage> images = catalog.imagesByProductId().getOrDefault(productId, List.of());
        if (images.isEmpty()) {
            return "";
        }
        if (item.getVariantId() != null) {
            Optional<ProductImage> variantImage = images.stream()
                    .filter(img -> item.getVariantId().equals(img.getVariantId()))
                    .findFirst();
            if (variantImage.isPresent() && hasText(variantImage.get().getImagePath())) {
                return resolveImageUrl(variantImage.get().getImagePath());
            }
        }
        Optional<ProductImage> primary = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst();
        ProductImage chosen = primary.orElse(images.get(0));
        return chosen != null && hasText(chosen.getImagePath())
                ? resolveImageUrl(chosen.getImagePath())
                : "";
    }

    private Long resolveEffectiveProductId(OrderItem item, LineCatalog catalog) {
        if (item.getProductId() != null) {
            return item.getProductId();
        }
        if (item.getVariantId() != null) {
            ProductVariant variant = catalog.variantsById().get(item.getVariantId());
            if (variant != null) {
                return variant.getProductId();
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildExtraNote(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getShiprocketAwbCode() != null && !order.getShiprocketAwbCode().isBlank()) {
            String courier = order.getShiprocketCourierName() != null ? order.getShiprocketCourierName() : "Courier";
            return "AWB: " + order.getShiprocketAwbCode() + "  |  Carrier: " + courier;
        }
        if (order.getShiprocketStatus() != null && !order.getShiprocketStatus().isBlank()) {
            return order.getShiprocketStatus();
        }
        return null;
    }

    private String primaryActionLabel(String status, OrderReviewSummaryDto reviewSummary) {
        if (reviewSummary != null && reviewSummary.isHasPendingReview()) {
            return "Review Request";
        }
        return switch (status) {
            case "Pending" -> "Accept Order";
            case "Processing" -> "Mark as Shipped";
            case "Shipped" -> "Mark as Delivered";
            case "Delivered" -> "Download Invoice";
            case "Returned" -> "Process Refund";
            case "Cancelled" -> "Download Invoice";
            default -> "View Order";
        };
    }

    private String secondaryActionLabel(String status, OrderReviewSummaryDto reviewSummary) {
        if (reviewSummary != null && reviewSummary.getReturnCount() > 0) {
            return "View Return Request";
        }
        if (reviewSummary != null && reviewSummary.getExchangeCount() > 0) {
            return "View Exchange Request";
        }
        if (reviewSummary != null && reviewSummary.getReplacementCount() > 0) {
            return "View Replacement Request";
        }
        if (reviewSummary != null && reviewSummary.getCancellationCount() > 0) {
            return "View Cancellation Request";
        }
        return switch (status) {
            case "Pending", "Processing" -> "Cancel Order";
            case "Shipped" -> "Track Shipment";
            case "Delivered" -> "Initiate Return";
            case "Returned" -> "View Return Request";
            case "Cancelled" -> "View Details";
            default -> "View Details";
        };
    }

    private String formatPaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return "Online Payment";
        }
        return switch (method.trim().toLowerCase(Locale.ROOT)) {
            case "cod", "cash_on_delivery" -> "Cash on Delivery";
            case "upi" -> "Online Payment (UPI)";
            case "card", "credit_card", "debit_card" -> "Credit Card";
            case "netbanking" -> "Net Banking";
            case "razorpay" -> "Online Payment (Razorpay)";
            default -> capitalize(method.replace('_', ' '));
        };
    }

    private String formatPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "paid", "success", "captured" -> "Paid";
            case "pending" -> "Pending";
            case "failed" -> "Failed";
            case "refunded" -> "Refunded";
            default -> capitalize(status);
        };
    }

    private BigDecimal sumSellerTotal(List<OrderItem> items) {
        return computeSellerItemTotals(items).total();
    }

    private BigDecimal sumLineTotals(List<OrderItem> items) {
        return items.stream()
                .map(this::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumField(List<OrderItem> items, java.util.function.Function<OrderItem, BigDecimal> getter) {
        return items.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Integer, List<OrderItemCustomDetail>> loadCustomDetailsByItem(List<Integer> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<OrderItemCustomDetail>> grouped = new HashMap<>();
        for (OrderItemCustomDetail detail : orderItemCustomDetailRepository.findByOrderItemIdInOrderByCreatedAtAsc(itemIds)) {
            grouped.computeIfAbsent(detail.getOrderItemId(), key -> new ArrayList<>()).add(detail);
        }
        return grouped;
    }

    private List<OrderEmailLogDto> loadEmailLogs(Long orderId) {
        return orderEmailLogRepository.findByOrderIdOrderBySentAtDesc(orderId).stream()
                .map(this::toEmailLogDto)
                .toList();
    }

    private List<OrderGstDto> loadGstRecords(Order order) {
        List<OrderGstDto> records = orderGstRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .map(this::toGstDto)
                .toList();
        if (!records.isEmpty()) {
            return records;
        }
        if ((order.getGstNumber() != null && !order.getGstNumber().isBlank())
                || (order.getGstInfo() != null && !order.getGstInfo().isBlank())) {
            return List.of(OrderGstDto.builder()
                    .orderId(order.getId())
                    .gstNumber(order.getGstNumber())
                    .gstInfo(order.getGstInfo())
                    .build());
        }
        return List.of();
    }

    private String resolveGstNumber(Order order, List<OrderGstDto> gstRecords) {
        if (gstRecords != null && !gstRecords.isEmpty() && gstRecords.get(0).getGstNumber() != null) {
            return gstRecords.get(0).getGstNumber();
        }
        return order.getGstNumber();
    }

    private String resolveGstInfo(Order order, List<OrderGstDto> gstRecords) {
        if (gstRecords != null && !gstRecords.isEmpty() && gstRecords.get(0).getGstInfo() != null) {
            return gstRecords.get(0).getGstInfo();
        }
        return order.getGstInfo();
    }

    private OrderEmailLogDto toEmailLogDto(OrderEmailLog log) {
        return OrderEmailLogDto.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .email(log.getEmail())
                .emailType(log.getEmailType())
                .subject(log.getSubject())
                .status(log.getStatus())
                .sentAt(log.getSentAt() != null ? formatDateTime(log.getSentAt()) : "")
                .errorMessage(log.getErrorMessage())
                .build();
    }

    private OrderGstDto toGstDto(OrderGst gst) {
        return OrderGstDto.builder()
                .id(gst.getId())
                .orderId(gst.getOrderId())
                .gstNumber(gst.getGstNumber())
                .gstInfo(gst.getGstInfo())
                .createdAt(gst.getCreatedAt() != null ? formatDateTime(gst.getCreatedAt()) : "")
                .updatedAt(gst.getUpdatedAt() != null ? formatDateTime(gst.getUpdatedAt()) : "")
                .build();
    }

    private OrderItemCustomDetailDto toCustomDetailDto(OrderItemCustomDetail detail) {
        return OrderItemCustomDetailDto.builder()
                .id(detail.getId())
                .orderItemId(detail.getOrderItemId())
                .fieldKey(detail.getFieldKey())
                .fieldLabel(detail.getFieldLabel())
                .valueText(detail.getValueText())
                .valueFile(detail.getValueFile() != null ? resolveImageUrl(detail.getValueFile()) : null)
                .createdAt(detail.getCreatedAt() != null ? formatDateTime(detail.getCreatedAt()) : "")
                .build();
    }

    private List<OrderReturnDto> toReturnDtos(List<OrderReturn> rows, Map<Integer, OrderItem> itemById) {
        return rows.stream().map(row -> OrderReturnDto.builder()
                .id(row.getId())
                .orderId(row.getOrderId())
                .orderItemId(row.getOrderItemId())
                .productName(productNameForItem(itemById, row.getOrderItemId()))
                .reason(row.getReason())
                .description(row.getDescription())
                .unboxingVideo(row.getUnboxingVideo() != null ? resolveImageUrl(row.getUnboxingVideo()) : null)
                .solution(row.getSolution())
                .solutionLabel(formatSolutionLabel(row.getSolution()))
                .status(row.getStatus())
                .statusLabel(formatReviewStatusLabel(row.getStatus()))
                .adminComment(row.getAdminComment())
                .shiprocketReturnId(row.getShiprocketReturnId())
                .processedAt(row.getProcessedAt() != null ? formatDateTime(row.getProcessedAt()) : null)
                .createdAt(row.getCreatedAt() != null ? formatDateTime(row.getCreatedAt()) : "")
                .updatedAt(row.getUpdatedAt() != null ? formatDateTime(row.getUpdatedAt()) : null)
                .build()).toList();
    }

    private List<OrderExchangeDto> toExchangeDtos(List<OrderExchange> rows, Map<Integer, OrderItem> itemById) {
        return rows.stream().map(row -> OrderExchangeDto.builder()
                .id(row.getId())
                .orderId(row.getOrderId())
                .orderItemId(row.getOrderItemId())
                .productName(productNameForItem(itemById, row.getOrderItemId()))
                .reason(row.getReason())
                .description(row.getDescription())
                .exchangeColor(row.getExchangeColor())
                .exchangeSize(row.getExchangeSize())
                .status(row.getStatus())
                .statusLabel(formatReviewStatusLabel(row.getStatus()))
                .adminComment(row.getAdminComment())
                .shiprocketOrderId(row.getShiprocketOrderId())
                .shiprocketShipmentId(row.getShiprocketShipmentId())
                .shiprocketAwbCode(row.getShiprocketAwbCode())
                .trackingNumber(row.getTrackingNumber())
                .shippingProvider(row.getShippingProvider())
                .processedAt(row.getProcessedAt() != null ? formatDateTime(row.getProcessedAt()) : null)
                .createdAt(row.getCreatedAt() != null ? formatDateTime(row.getCreatedAt()) : "")
                .build()).toList();
    }

    private List<OrderReplacementDto> toReplacementDtos(List<OrderReplacement> rows, Map<Integer, OrderItem> itemById) {
        return rows.stream().map(row -> OrderReplacementDto.builder()
                .id(row.getId())
                .orderId(row.getOrderId())
                .orderItemId(row.getOrderItemId())
                .productName(productNameForItem(itemById, row.getOrderItemId()))
                .reason(row.getReason())
                .description(row.getDescription())
                .status(row.getStatus())
                .statusLabel(formatReviewStatusLabel(row.getStatus()))
                .adminComment(row.getAdminComment())
                .shiprocketReturnId(row.getShiprocketReturnId())
                .trackingNumber(row.getTrackingNumber())
                .shippingProvider(row.getShippingProvider())
                .processedAt(row.getProcessedAt() != null ? formatDateTime(row.getProcessedAt()) : null)
                .createdAt(row.getCreatedAt() != null ? formatDateTime(row.getCreatedAt()) : "")
                .build()).toList();
    }

    private List<OrderItemCancellationDto> toCancellationDtos(
            List<OrderItemCancellation> rows, Map<Integer, OrderItem> itemById) {
        return rows.stream().map(row -> OrderItemCancellationDto.builder()
                .id(row.getId())
                .orderId(row.getOrderId())
                .orderItemId(row.getOrderItemId())
                .productName(productNameForItem(itemById, row.getOrderItemId()))
                .reason(row.getReason())
                .status(row.getStatus())
                .statusLabel(formatReviewStatusLabel(row.getStatus()))
                .adminComment(row.getAdminComment())
                .processedAt(row.getProcessedAt() != null ? formatDateTime(row.getProcessedAt()) : null)
                .createdAt(row.getCreatedAt() != null ? formatDateTime(row.getCreatedAt()) : "")
                .build()).toList();
    }

    private OrderReviewSummaryDto buildReviewSummary(
            List<OrderReturnDto> returns,
            List<OrderExchangeDto> exchanges,
            List<OrderReplacementDto> replacements,
            List<OrderItemCancellationDto> cancellations) {
        int returnCount = returns.size();
        int exchangeCount = exchanges.size();
        int replacementCount = replacements.size();
        int cancellationCount = cancellations.size();
        boolean hasPending = returns.stream().anyMatch(r -> isPendingReviewStatus(r.getStatus()))
                || exchanges.stream().anyMatch(e -> isPendingReviewStatus(e.getStatus()))
                || replacements.stream().anyMatch(r -> isPendingReviewStatus(r.getStatus()))
                || cancellations.stream().anyMatch(c -> isPendingReviewStatus(c.getStatus()));
        return OrderReviewSummaryDto.builder()
                .returnCount(returnCount)
                .exchangeCount(exchangeCount)
                .replacementCount(replacementCount)
                .cancellationCount(cancellationCount)
                .hasPendingReview(hasPending)
                .build();
    }

    private boolean isPendingReviewStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return "pending".equalsIgnoreCase(status.trim());
    }

    private String productNameForItem(Map<Integer, OrderItem> itemById, Integer orderItemId) {
        if (orderItemId == null) {
            return "Product";
        }
        OrderItem item = itemById.get(orderItemId);
        if (item == null) {
            return "Product";
        }
        return resolveProductName(item, loadProductNames(List.of(item)));
    }

    private String formatReviewStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pending" -> "Pending";
            case "approved" -> "Approved";
            case "rejected" -> "Rejected";
            case "shipped" -> "Shipped";
            case "completed" -> "Completed";
            default -> capitalize(status.replace('_', ' '));
        };
    }

    private String formatSolutionLabel(String solution) {
        if (solution == null || solution.isBlank()) {
            return "";
        }
        return switch (solution.trim().toLowerCase(Locale.ROOT)) {
            case "refund" -> "Refund";
            case "store_credit" -> "Store Credit";
            default -> capitalize(solution.replace('_', ' '));
        };
    }

    private LocalDateTime latestItemTime(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
    }

    /**
     * Orders are persisted as UTC wall-clock {@link LocalDateTime} (same as user-service).
     * Display in Asia/Kolkata so seller timing matches the buyer / admin experience.
     */
    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(DISPLAY_ZONE)
                .format(DISPLAY_DATE_TIME);
    }

    private String formatStepDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(DISPLAY_ZONE)
                .format(STEP_DATE);
    }

    private String formatInr(BigDecimal amount) {
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        synchronized (INR) {
            INR.setMaximumFractionDigits(0);
            INR.setMinimumFractionDigits(0);
            return INR.format(safe).replace("₹", "₹");
        }
    }

    private String resolveImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = mediaBaseUrl != null ? mediaBaseUrl.trim() : "";
        if (base.isBlank()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String joinNonBlank(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String nullToDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    private record StepDef(String key, String label, String iconLib, String iconName) {
    }
}
