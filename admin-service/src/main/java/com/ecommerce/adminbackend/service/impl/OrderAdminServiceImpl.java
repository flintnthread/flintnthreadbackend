package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.config.InvoiceSettings;
import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.OrderItem;
import com.ecommerce.adminbackend.entity.OrderStatusHistory;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductImage;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.OrderStatusHistoryRepository;
import com.ecommerce.adminbackend.repository.ProductImageRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.MailService;
import com.ecommerce.adminbackend.service.OrderAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import com.ecommerce.adminbackend.util.QrCodeGenerator;
import com.ecommerce.adminbackend.util.InvoiceHtmlBuilder;
import com.ecommerce.adminbackend.util.InvoicePdfRenderer;
import com.ecommerce.adminbackend.util.ShippingLabelHtmlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAdminServiceImpl extends BaseAdminService implements OrderAdminService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    /** Matches admin order cards: "15 Jul 2026 - 03:40 pm" */
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy - hh:mm a", Locale.ENGLISH);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final MediaUrlHelper mediaUrlHelper;
    private final InvoiceSettings invoiceSettings;
    private final MailService mailService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listOrders(
            String status,
            String paymentStatus,
            String paymentMethod,
            String search,
            String sort,
            Long sellerId,
            int page,
            int size) {
        var result = orderRepository.searchOrders(
                blankToNull(status),
                blankToNull(paymentStatus),
                blankToNull(paymentMethod),
                blankToNull(search),
                sellerId,
                PageRequest.of(page, size, resolveOrderSort(sort)));
        List<Order> orders = result.getContent();
        if (orders.isEmpty()) {
            return PageResponse.from(result.map(this::toOrderSummary));
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, String> productImageById = resolveProductImages(itemsByOrder);
        List<OrderItem> allItems = itemsByOrder.values().stream().flatMap(List::stream).toList();
        Map<Long, Seller> sellersById = resolveSellers(allItems);
        Map<Long, String> productNameById = resolveProductNames(allItems);
        Map<Long, Product> productsById = resolveProducts(allItems);

        return PageResponse.from(result.map(order -> {
            Map<String, Object> summary = toOrderSummary(order);
            List<OrderItem> items = itemsByOrder.getOrDefault(order.getId(), List.of());
            summary.put("products", buildProductPreviews(items, productImageById, productNameById));
            summary.put("sellers", buildSellerPreviews(items, sellersById));
            List<Map<String, Object>> sellerGroups = buildInvoiceSellerGroups(
                    items, productsById, sellersById, productImageById, productNameById);
            enrichOrderDocumentFlags(summary, order, items, sellerGroups);
            appendListShippingMeta(summary, order, items);
            return summary;
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("thisMonth", orderRepository.countSince(monthStart));
        stats.put("monthRevenue", orderRepository.sumTotalAmountSince(monthStart));
        stats.put("pendingPayments", orderRepository.countPendingPayments());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportOrdersCsv(
            String status,
            String paymentStatus,
            String paymentMethod,
            String search,
            String sort) {
        var result = orderRepository.searchOrders(
                blankToNull(status),
                blankToNull(paymentStatus),
                blankToNull(paymentMethod),
                blankToNull(search),
                null,
                PageRequest.of(0, 10_000, resolveOrderSort(sort)));
        List<Order> orders = result.getContent();

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(csvHeader(
                "Order ID", "Order Number", "Order Date", "Order Status",
                "Customer Name", "Customer Email", "Customer Phone",
                "Shipping Address", "City", "State", "Pincode",
                "Total Amount", "Tax Amount", "Shipping Amount",
                "Payment Method", "Payment Status", "GST Status",
                "AWB Code", "Courier", "Tracking URL"
        )).append('\n');

        for (Order order : orders) {
            String address = joinNonBlank(
                    order.getShippingAddress1(),
                    order.getShippingAddress2());
            csv.append(csvEscape(order.getId())).append(',');
            csv.append(csvEscape(order.getOrderNumber())).append(',');
            csv.append(csvEscape(formatOrderDateTimeIst(order.getCreatedAt()))).append(',');
            csv.append(csvEscape(order.getOrderStatus())).append(',');
            csv.append(csvEscape(order.getShippingName())).append(',');
            csv.append(csvEscape(order.getShippingEmail())).append(',');
            csv.append(csvEscape(order.getShippingPhone())).append(',');
            csv.append(csvEscape(address)).append(',');
            csv.append(csvEscape(order.getShippingCity())).append(',');
            csv.append(csvEscape(order.getShippingState())).append(',');
            csv.append(csvEscape(order.getShippingPincode())).append(',');
            csv.append(csvEscape(order.getTotalAmount())).append(',');
            csv.append(csvEscape(order.getTaxAmount())).append(',');
            csv.append(csvEscape(order.getShippingAmount())).append(',');
            csv.append(csvEscape(order.getPaymentMethod())).append(',');
            csv.append(csvEscape(order.getPaymentStatus())).append(',');
            csv.append(csvEscape(resolveGstStatus(order))).append(',');
            csv.append(csvEscape(order.getShiprocketAwbCode())).append(',');
            csv.append(csvEscape(order.getShiprocketCourierName())).append(',');
            csv.append(csvEscape(order.getShiprocketTrackingUrl())).append('\n');
        }
        return csv.toString();
    }

    private String joinNonBlank(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOrder(Long id) {
        Order order = requireOrder(id);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        Map<Long, String> productImageById = resolveProductImages(Map.of(id, items));
        Map<Long, String> productNameById = resolveProductNames(items);
        Map<Long, Seller> sellersById = resolveSellers(items);
        List<OrderStatusHistory> statusHistory = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(id);

        Map<String, Object> detail = toOrderDetail(order);
        detail.put("customerId", resolveCustomerAnchorId(order));
        detail.put("items", items.stream().map(item -> toItem(item, productImageById, productNameById)).toList());
        detail.put("statusHistory", statusHistory.stream().map(this::toStatusHistoryRow).toList());
        detail.put("sellers", buildSellerPreviews(items, sellersById));
        List<Map<String, Object>> sellerGroups = buildInvoiceSellerGroups(
                items, resolveProducts(items), sellersById, productImageById, productNameById);
        enrichOrderDocumentFlags(detail, order, items, sellerGroups);
        appendListShippingMeta(detail, order, items);
        return detail;
    }

    @Override
    @Transactional
    public Map<String, Object> updateOrderStatus(
            Long id, String status, String comment, Long adminUserId, boolean notifyCustomer) {
        Order order = requireOrder(id);
        String dbStatus = toDbStatus(status);
        String historyStatus = toHistoryStatus(status);
        LocalDateTime now = LocalDateTime.now();

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            item.setStatus(dbStatus);
        }
        orderItemRepository.saveAll(items);

        order.setOrderStatus(dbStatus);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrderId(id);
        entry.setStatus(historyStatus);
        entry.setComment(comment != null && !comment.isBlank() ? comment.trim() : null);
        entry.setCreatedBy(adminUserId);
        entry.setCreatedAt(now);
        orderStatusHistoryRepository.save(entry);

        if (notifyCustomer) {
            try {
                String orderNumber = order.getOrderNumber() != null && !order.getOrderNumber().isBlank()
                        ? order.getOrderNumber()
                        : "#" + order.getId();
                mailService.sendOrderStatusUpdateEmail(
                        order.getShippingEmail(),
                        order.getShippingName(),
                        orderNumber,
                        toDisplayStatus(historyStatus),
                        comment);
            } catch (Exception ex) {
                log.warn("Customer notification email failed for order {}: {}", id, ex.getMessage());
            }
        }

        return getOrder(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateInvoice(Long id) {
        Order order = requireOrder(id);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        Map<Long, String> productImageById = resolveProductImages(Map.of(id, items));
        Map<Long, String> productNameById = resolveProductNames(items);
        Map<Long, Product> productsById = resolveProducts(items);
        Map<Long, Seller> sellersById = resolveSellers(items);

        List<Map<String, Object>> sellerGroups = buildInvoiceSellerGroups(
                items, productsById, sellersById, productImageById, productNameById);
        Map<String, Object> totals = buildInvoiceTotals(order, sellerGroups);
        boolean isIntraState = resolveIntraState(order, items, sellersById);
        String orderDetailsUrl = buildOrderDetailsUrl(id);
        String invoiceNumber = buildInvoiceNumber(order);

        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("orderId", id);
        invoice.put("orderNumber", order.getOrderNumber());
        invoice.put("invoiceDate", formatOrderDateTimeIst(LocalDateTime.now(ZoneOffset.UTC)));
        invoice.put("orderDate", formatOrderDateTimeIst(order.getCreatedAt()));
        invoice.put("company", invoiceSettings.toCompanyMap());
        invoice.put("billing", buildCustomerAddress(
                order.getBillingName(),
                order.getBillingEmail(),
                order.getBillingPhone(),
                order.getBillingAddress1(),
                order.getBillingAddress2(),
                order.getBillingCity(),
                order.getBillingState(),
                order.getBillingPincode(),
                order.getBillingCountry()));
        invoice.put("shipping", buildCustomerAddress(
                order.getShippingName(),
                order.getShippingEmail(),
                order.getShippingPhone(),
                order.getShippingAddress1(),
                order.getShippingAddress2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPincode(),
                order.getShippingCountry()));
        invoice.put("sellerGroups", sellerGroups);
        invoice.put("payment", Map.of(
                "method", nullSafe(order.getPaymentMethod()),
                "status", nullSafe(order.getPaymentStatus()),
                "paid", isPaymentPaid(order.getPaymentStatus())));
        invoice.put("orderStatus", nullSafe(order.getOrderStatus()));
        invoice.put("gstStatus", resolveGstStatus(order));
        invoice.put("gstNumber", nullSafe(order.getGstNumber()));
        invoice.put("totals", totals);
        invoice.put("isIntraState", isIntraState);
        invoice.put("gstBreakdown", buildGstBreakdown(totals, isIntraState));
        invoice.put("qrCode", Map.of(
                "url", orderDetailsUrl,
                "imageDataUrl", QrCodeGenerator.toBase64PngDataUrl(orderDetailsUrl, 200)));
        return invoice;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long id) {
        Map<String, Object> invoice = generateInvoice(id);
        String html = InvoiceHtmlBuilder.build(invoice);
        return InvoicePdfRenderer.renderPdf(html);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateShippingLabel(Long id) {
        Order order = requireOrder(id);
        Map<String, Object> label = new LinkedHashMap<>(generateInvoice(id));
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        appendListShippingMeta(label, order, items);
        label.put("awbCode", nullSafe(order.getShiprocketAwbCode()));
        label.put("courierName", !isBlank(order.getShiprocketCourierName())
                ? order.getShiprocketCourierName().trim()
                : "Courier");
        label.put("trackingUrl", nullSafe(order.getShiprocketTrackingUrl()));
        return label;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateShippingLabelPdf(Long id) {
        Map<String, Object> label = generateShippingLabel(id);
        String html = ShippingLabelHtmlBuilder.build(label);
        return InvoicePdfRenderer.renderPdf(html);
    }

    @Override
    @Transactional
    public Map<String, Object> updateGstStatus(Long id, String gstStatus) {
        Order order = requireOrder(id);
        String normalized = normalizeGstStatus(requireNonBlank(gstStatus, "GST status"));
        order.setGstInfo(normalized);
        orderRepository.save(order);
        return Map.of("orderId", id, "gstStatus", normalized, "message", "GST status updated.");
    }

    private String normalizeGstStatus(String gstStatus) {
        String value = gstStatus.trim();
        if ("filed".equalsIgnoreCase(value)) {
            return "Filed";
        }
        if ("pending".equalsIgnoreCase(value) || "not filed".equalsIgnoreCase(value)) {
            return "Not Filed";
        }
        throw new IllegalArgumentException("Unsupported GST status: " + gstStatus);
    }

    private Order requireOrder(Long id) {
        return requireFound(orderRepository.findById(id), "Order not found.");
    }

    private String resolveGstStatus(Order order) {
        if (order.getGstInfo() != null && !order.getGstInfo().isBlank()) {
            return order.getGstInfo();
        }
        return "Not Filed";
    }

    private Map<String, Object> toOrderDetail(Order order) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", order.getId());
        detail.put("userId", order.getUserId());
        detail.put("orderNumber", order.getOrderNumber());
        detail.put("orderStatus", order.getOrderStatus());
        detail.put("paymentStatus", order.getPaymentStatus());
        detail.put("paymentMethod", order.getPaymentMethod());
        detail.put("sellerPaymentStatus", order.getSellerPaymentStatus());
        detail.put("totalAmount", order.getTotalAmount());
        detail.put("shippingAmount", order.getShippingAmount());
        detail.put("taxAmount", order.getTaxAmount());
        detail.put("discountAmount", order.getDiscountAmount());
        detail.put("walletDeduction", order.getWalletDeduction());
        detail.put("referralDiscountAmount", order.getReferralDiscountAmount());
        detail.put("referralDiscountPercent", order.getReferralDiscountPercent());
        detail.put("shippingName", order.getShippingName());
        detail.put("shippingEmail", order.getShippingEmail());
        detail.put("shippingPhone", order.getShippingPhone());
        detail.put("shippingAddress1", order.getShippingAddress1());
        detail.put("shippingAddress2", order.getShippingAddress2());
        detail.put("shippingCity", order.getShippingCity());
        detail.put("shippingState", order.getShippingState());
        detail.put("shippingCountry", order.getShippingCountry());
        detail.put("shippingPincode", order.getShippingPincode());
        detail.put("billingName", order.getBillingName());
        detail.put("billingEmail", order.getBillingEmail());
        detail.put("billingPhone", order.getBillingPhone());
        detail.put("billingAddress1", order.getBillingAddress1());
        detail.put("billingAddress2", order.getBillingAddress2());
        detail.put("billingCity", order.getBillingCity());
        detail.put("billingState", order.getBillingState());
        detail.put("billingCountry", order.getBillingCountry());
        detail.put("billingPincode", order.getBillingPincode());
        detail.put("orderNotes", order.getOrderNotes());
        detail.put("gstNumber", order.getGstNumber());
        detail.put("gstInfo", order.getGstInfo());
        detail.put("gstStatus", resolveGstStatus(order));
        detail.put("razorpayOrderId", order.getRazorpayOrderId());
        detail.put("razorpayPaymentId", order.getRazorpayPaymentId());
        detail.put("shiprocketOrderId", order.getShiprocketOrderId());
        detail.put("shiprocketShipmentId", order.getShiprocketShipmentId());
        detail.put("shiprocketAwbCode", order.getShiprocketAwbCode());
        detail.put("shiprocketCourierName", order.getShiprocketCourierName());
        detail.put("shiprocketStatus", order.getShiprocketStatus());
        detail.put("shiprocketTrackingUrl", order.getShiprocketTrackingUrl());
        detail.put("shiprocketPushedAt", toUtcIso(order.getShiprocketPushedAt()));
        detail.put("shiprocketSyncedAt", toUtcIso(order.getShiprocketSyncedAt()));
        detail.put("createdAt", toUtcIso(order.getCreatedAt()));
        detail.put("createdAtDisplay", formatOrderDateTimeIst(order.getCreatedAt()));
        detail.put("updatedAt", toUtcIso(order.getUpdatedAt()));
        return detail;
    }

    private Map<String, Object> toStatusHistoryRow(OrderStatusHistory entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entry.getId());
        row.put("status", entry.getStatus());
        row.put("comment", entry.getComment());
        row.put("createdBy", entry.getCreatedBy());
        row.put("createdAt", toUtcIso(entry.getCreatedAt()));
        row.put("createdAtDisplay", formatOrderDateTimeIst(entry.getCreatedAt()));
        return row;
    }

    private Long resolveCustomerAnchorId(Order order) {
        if (order.getUserId() != null) {
            return order.getUserId();
        }
        return order.getId();
    }

    private Map<String, Object> toOrderSummary(Order order) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", order.getId());
        summary.put("orderNumber", order.getOrderNumber());
        summary.put("orderStatus", order.getOrderStatus());
        summary.put("paymentStatus", order.getPaymentStatus());
        summary.put("paymentMethod", order.getPaymentMethod());
        summary.put("totalAmount", order.getTotalAmount());
        summary.put("shippingName", order.getShippingName());
        summary.put("shippingEmail", order.getShippingEmail());
        summary.put("shippingPhone", order.getShippingPhone());
        summary.put("shippingCity", order.getShippingCity());
        summary.put("shippingState", order.getShippingState());
        summary.put("shippingAddress1", order.getShippingAddress1());
        summary.put("shippingAddress2", order.getShippingAddress2());
        summary.put("shippingPincode", order.getShippingPincode());
        summary.put("gstStatus", resolveGstStatus(order));
        summary.put("createdAt", toUtcIso(order.getCreatedAt()));
        summary.put("createdAtDisplay", formatOrderDateTimeIst(order.getCreatedAt()));
        summary.put("shiprocketAwbCode", order.getShiprocketAwbCode());
        summary.put("shiprocketCourierName", order.getShiprocketCourierName());
        summary.put("shiprocketTrackingUrl", order.getShiprocketTrackingUrl());
        return summary;
    }

    private void enrichOrderDocumentFlags(
            Map<String, Object> target,
            Order order,
            List<OrderItem> items,
            List<Map<String, Object>> sellerGroups) {
        boolean hasInvoice = items != null && !items.isEmpty();
        boolean hasShippingLabel = !isBlank(order.getShiprocketAwbCode());
        target.put("hasInvoice", hasInvoice);
        target.put("hasShippingLabel", hasShippingLabel);
        for (Map<String, Object> group : sellerGroups) {
            group.put("hasInvoice", hasInvoice);
            group.put("hasShippingLabel", hasShippingLabel);
        }
        target.put("sellerGroups", sellerGroups);
    }

    private void appendListShippingMeta(Map<String, Object> summary, Order order, List<OrderItem> items) {
        BigDecimal weightKg = BigDecimal.ZERO;
        BigDecimal maxLength = BigDecimal.ZERO;
        BigDecimal maxWidth = BigDecimal.ZERO;
        BigDecimal maxHeight = BigDecimal.ZERO;

        for (OrderItem item : items) {
            if (item.getChargeableWeight() != null) {
                weightKg = weightKg.add(item.getChargeableWeight());
            } else if (item.getPackageDeadWeight() != null) {
                weightKg = weightKg.add(item.getPackageDeadWeight());
            } else if (item.getWeight() != null) {
                weightKg = weightKg.add(item.getWeight());
            }
            if (item.getLengthCm() != null && item.getLengthCm().compareTo(maxLength) > 0) {
                maxLength = item.getLengthCm();
            }
            if (item.getWidthCm() != null && item.getWidthCm().compareTo(maxWidth) > 0) {
                maxWidth = item.getWidthCm();
            }
            if (item.getHeightCm() != null && item.getHeightCm().compareTo(maxHeight) > 0) {
                maxHeight = item.getHeightCm();
            }
        }

        summary.put("weightKg", weightKg);
        if (maxLength.compareTo(BigDecimal.ZERO) > 0
                || maxWidth.compareTo(BigDecimal.ZERO) > 0
                || maxHeight.compareTo(BigDecimal.ZERO) > 0) {
            summary.put("dimensionsCm", Map.of(
                    "l", maxLength,
                    "w", maxWidth,
                    "h", maxHeight));
        }
        summary.put("trackingId", order.getShiprocketAwbCode());
    }

    private Sort resolveOrderSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "amount_high" -> Sort.by(Sort.Direction.DESC, "totalAmount");
            case "amount_low" -> Sort.by(Sort.Direction.ASC, "totalAmount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Map<String, Object> toItem(
            OrderItem item,
            Map<Long, String> productImageById,
            Map<Long, String> productNameById) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("productId", item.getProductId());
        row.put("productName", resolveItemProductName(item, productNameById));
        row.put("sku", item.getSku());
        row.put("color", item.getColor());
        row.put("size", item.getSize());
        row.put("quantity", item.getQuantity());
        row.put("price", item.getPrice());
        row.put("total", item.getTotal());
        row.put("status", item.getStatus());
        row.put("sellerId", item.getSellerId());
        row.put("sellerName", item.getSellerName());
        row.put("imageUrl", resolveItemImageUrl(item, productImageById));
        return row;
    }

    private Map<Long, String> resolveProductNames(List<OrderItem> items) {
        Set<Long> productIds = items.stream()
                .filter(item -> item.getProductId() != null && isBlank(item.getProductName()))
                .map(OrderItem::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> names = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            if (!isBlank(product.getName())) {
                names.put(product.getId(), product.getName());
            }
        }
        return names;
    }

    private String resolveItemProductName(OrderItem item, Map<Long, String> productNameById) {
        if (!isBlank(item.getProductName())) {
            return item.getProductName().trim();
        }
        if (item.getProductId() != null) {
            String fromCatalog = productNameById.get(item.getProductId());
            if (!isBlank(fromCatalog)) {
                return fromCatalog.trim();
            }
        }
        return item.getProductId() != null ? "Product #" + item.getProductId() : "Product";
    }

    private Map<Long, String> resolveProductImages(Map<Long, List<OrderItem>> itemsByOrder) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (List<OrderItem> items : itemsByOrder.values()) {
            for (OrderItem item : items) {
                if (item.getProductId() != null && isBlank(item.getProductImagePath())) {
                    productIds.add(item.getProductId());
                }
            }
        }
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ProductImage>> imagesByProduct = productImageRepository
                .findByProductIdInOrderBySortOrderAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        Map<Long, String> imageByProductId = new HashMap<>();
        for (Map.Entry<Long, List<ProductImage>> entry : imagesByProduct.entrySet()) {
            String url = entry.getValue().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                    .findFirst()
                    .or(() -> entry.getValue().stream().findFirst())
                    .map(image -> mediaUrlHelper.toPublicUrl(image.getImagePath()))
                    .orElse("");
            if (!url.isBlank()) {
                imageByProductId.put(entry.getKey(), url);
            }
        }
        return imageByProductId;
    }

    private List<Map<String, Object>> buildProductPreviews(
            List<OrderItem> items,
            Map<Long, String> productImageById,
            Map<Long, String> productNameById) {
        List<Map<String, Object>> products = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("productId", item.getProductId());
            row.put("name", resolveItemProductName(item, productNameById));
            row.put("productName", resolveItemProductName(item, productNameById));
            row.put("imageUrl", resolveItemImageUrl(item, productImageById));
            row.put("sellerName", item.getSellerName());
            row.put("sellerId", item.getSellerId());
            row.put("price", item.getPrice());
            row.put("quantity", item.getQuantity());
            row.put("qty", item.getQuantity());
            row.put("hsnCode", item.getHsnCode());
            row.put("color", item.getColor());
            row.put("size", item.getSize());
            row.put("sku", item.getSku());
            products.add(row);
        }
        return products;
    }

    private List<Map<String, Object>> buildSellerPreviews(List<OrderItem> items, Map<Long, Seller> sellersById) {
        Map<Long, Map<String, Object>> sellers = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getSellerId() == null) {
                continue;
            }
            sellers.computeIfAbsent(item.getSellerId(), sellerId -> {
                Seller seller = sellersById.get(sellerId);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", sellerId);
                if (seller != null) {
                    String name = !isBlank(seller.getBusinessName())
                            ? seller.getBusinessName()
                            : ((nullSafe(seller.getFirstName()) + " " + nullSafe(seller.getLastName())).trim());
                    row.put("name", isBlank(name) ? nullSafe(item.getSellerName()) : name);
                    row.put("email", nullSafe(seller.getEmail()));
                } else {
                    row.put("name", item.getSellerName() != null ? item.getSellerName() : "Seller");
                    row.put("email", "");
                }
                return row;
            });
        }
        return new ArrayList<>(sellers.values());
    }

    private String resolveItemImageUrl(OrderItem item, Map<Long, String> productImageById) {
        if (!isBlank(item.getProductImagePath())) {
            return mediaUrlHelper.toPublicUrl(item.getProductImagePath());
        }
        if (item.getProductId() != null) {
            String fallback = productImageById.get(item.getProductId());
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toDbStatus(String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        return switch (uiStatus.trim()) {
            case "Pending", "Sent to Seller" -> "confirmed";
            case "Processing" -> "processing";
            case "Completed" -> "delivered";
            case "Returned" -> "returned";
            case "Replacement" -> "replacement";
            case "Cancelled" -> "cancelled";
            default -> uiStatus.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String toHistoryStatus(String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank()) {
            return "pending";
        }
        return switch (uiStatus.trim()) {
            case "Pending" -> "pending";
            case "Sent to Seller" -> "sent_to_seller";
            case "Processing" -> "processing";
            case "Completed" -> "completed";
            case "Returned" -> "returned";
            case "Replacement" -> "replacement";
            case "Cancelled" -> "cancelled";
            default -> uiStatus.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String toDisplayStatus(String historyStatus) {
        if (historyStatus == null || historyStatus.isBlank()) {
            return "Updated";
        }
        return switch (historyStatus.trim().toLowerCase(Locale.ROOT)) {
            case "pending" -> "Pending";
            case "sent_to_seller" -> "Sent to Seller";
            case "processing" -> "Processing";
            case "completed", "delivered" -> "Completed";
            case "returned" -> "Returned";
            case "replacement" -> "Replacement";
            case "cancelled" -> "Cancelled";
            default -> historyStatus.replace('_', ' ');
        };
    }

    private Map<Long, Product> resolveProducts(List<OrderItem> items) {
        Set<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            productsById.put(product.getId(), product);
        }
        return productsById;
    }

    private Map<Long, Seller> resolveSellers(List<OrderItem> items) {
        Set<Long> sellerIds = items.stream()
                .map(OrderItem::getSellerId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Seller> sellersById = new HashMap<>();
        for (Seller seller : sellerRepository.findAllById(sellerIds)) {
            sellersById.put(seller.getId(), seller);
        }
        return sellersById;
    }

    private List<Map<String, Object>> buildInvoiceSellerGroups(
            List<OrderItem> items,
            Map<Long, Product> productsById,
            Map<Long, Seller> sellersById,
            Map<Long, String> productImageById,
            Map<Long, String> productNameById) {
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();

        for (OrderItem item : items) {
            String sellerKey = item.getSellerId() != null
                    ? "seller-" + item.getSellerId()
                    : "seller-" + nullSafe(item.getSellerName());
            groups.computeIfAbsent(sellerKey, key -> {
                Seller seller = item.getSellerId() != null ? sellersById.get(item.getSellerId()) : null;
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("seller", buildSellerInfo(item, seller));
                group.put("products", new ArrayList<Map<String, Object>>());
                return group;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products =
                    (List<Map<String, Object>>) groups.get(sellerKey).get("products");
            products.add(buildInvoiceLineItem(item, productsById, productImageById, productNameById));
        }

        return new ArrayList<>(groups.values());
    }

    private Map<String, Object> buildSellerInfo(OrderItem item, Seller seller) {
        Map<String, Object> sellerInfo = new LinkedHashMap<>();
        String sellerName = seller != null && !isBlank(seller.getBusinessName())
                ? seller.getBusinessName()
                : (seller != null
                ? ((nullSafe(seller.getFirstName()) + " " + nullSafe(seller.getLastName())).trim())
                : nullSafe(item.getSellerName()));
        if (isBlank(sellerName)) {
            sellerName = "Seller";
        }

        Map<String, Object> address = new LinkedHashMap<>();
        if (seller != null) {
            address.put("line1", nullSafe(seller.getAddress()));
            address.put("city", nullSafe(seller.getCity()));
            address.put("state", nullSafe(seller.getState()));
            address.put("pincode", nullSafe(seller.getPincode()));
            address.put("phone", nullSafe(seller.getMobile()));
            address.put("email", nullSafe(seller.getEmail()));
            address.put("gstin", nullSafe(seller.getGstNumber()));
        }

        sellerInfo.put("name", sellerName);
        sellerInfo.put("email", seller != null ? nullSafe(seller.getEmail()) : "");
        sellerInfo.put("phone", seller != null ? nullSafe(seller.getMobile()) : "");
        sellerInfo.put("gstin", seller != null ? nullSafe(seller.getGstNumber()) : "");
        sellerInfo.put("address", address);
        return sellerInfo;
    }

    private Map<String, Object> buildInvoiceLineItem(
            OrderItem item,
            Map<Long, Product> productsById,
            Map<Long, String> productImageById,
            Map<Long, String> productNameById) {
        Product product = item.getProductId() != null ? productsById.get(item.getProductId()) : null;
        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal taxPercent = resolveTaxPercent(item, product);
        BigDecimal taxAmount = lineSubtotal
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = lineSubtotal.add(taxAmount);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("productId", item.getProductId());
        row.put("name", resolveItemProductName(item, productNameById));
        row.put("imageUrl", resolveItemImageUrl(item, productImageById));
        row.put("sku", nullSafe(item.getSku()));
        row.put("hsnCode", !isBlank(item.getHsnCode())
                ? item.getHsnCode()
                : (product != null ? nullSafe(product.getHsnCode()) : ""));
        row.put("color", nullSafe(item.getColor()));
        row.put("size", nullSafe(item.getSize()));
        row.put("qty", qty);
        row.put("unitPrice", unitPrice);
        row.put("taxPercent", taxPercent);
        row.put("lineSubtotal", lineSubtotal);
        row.put("taxAmount", taxAmount);
        row.put("lineTotal", lineTotal);
        return row;
    }

    private BigDecimal resolveTaxPercent(OrderItem item, Product product) {
        if (product != null && product.getGstPercentage() != null) {
            return product.getGstPercentage();
        }
        return BigDecimal.valueOf(5);
    }

    private Map<String, Object> buildInvoiceTotals(Order order, List<Map<String, Object>> sellerGroups) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map<String, Object> group : sellerGroups) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) group.get("products");
            for (Map<String, Object> product : products) {
                subtotal = subtotal.add(toBigDecimal(product.get("lineSubtotal")));
                totalTax = totalTax.add(toBigDecimal(product.get("taxAmount")));
                grandTotal = grandTotal.add(toBigDecimal(product.get("lineTotal")));
            }
        }

        BigDecimal shipping = order.getShippingAmount() != null ? order.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal wallet = order.getWalletDeduction() != null ? order.getWalletDeduction() : BigDecimal.ZERO;
        BigDecimal referral = order.getReferralDiscountAmount() != null
                ? order.getReferralDiscountAmount()
                : BigDecimal.ZERO;
        BigDecimal orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() : grandTotal.add(shipping);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subtotal", subtotal);
        totals.put("tax", totalTax);
        totals.put("shipping", shipping);
        totals.put("discount", discount);
        totals.put("walletDeduction", wallet);
        totals.put("referralDiscount", referral);
        totals.put("grandTotal", orderTotal);
        return totals;
    }

    private Map<String, Object> buildGstBreakdown(Map<String, Object> totals, boolean isIntraState) {
        BigDecimal totalTax = toBigDecimal(totals.get("tax"));
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("cgst", isIntraState ? totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        breakdown.put("sgst", isIntraState ? totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        breakdown.put("igst", isIntraState ? BigDecimal.ZERO : totalTax);
        breakdown.put("total", totalTax);
        return breakdown;
    }

    private Map<String, Object> buildCustomerAddress(
            String name,
            String email,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String pincode,
            String country) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("name", nullSafe(name));
        address.put("email", nullSafe(email));
        address.put("phone", nullSafe(phone));
        address.put("line1", nullSafe(line1));
        address.put("line2", nullSafe(line2));
        address.put("city", nullSafe(city));
        address.put("state", nullSafe(state));
        address.put("pincode", nullSafe(pincode));
        address.put("country", isBlank(country) ? "India" : country);
        address.put("address", String.join(" ",
                nullSafe(line1),
                nullSafe(line2)).trim());
        return address;
    }

    private boolean resolveIntraState(Order order, List<OrderItem> items, Map<Long, Seller> sellersById) {
        String buyerState = nullSafe(order.getShippingState());
        if (isBlank(buyerState)) {
            buyerState = nullSafe(order.getBillingState());
        }
        if (isBlank(buyerState)) {
            return false;
        }

        for (OrderItem item : items) {
            if (item.getSellerId() == null) {
                continue;
            }
            Seller seller = sellersById.get(item.getSellerId());
            if (seller != null && !isBlank(seller.getState())) {
                return buyerState.equalsIgnoreCase(seller.getState().trim());
            }
        }
        return false;
    }

    private String buildInvoiceNumber(Order order) {
        String digits = order.getOrderNumber() != null
                ? order.getOrderNumber().replaceAll("\\D", "")
                : "";
        if (digits.isBlank()) {
            digits = String.valueOf(order.getId());
        }
        return "INV-" + digits;
    }

    private String buildOrderDetailsUrl(Long orderId) {
        String base = invoiceSettings.getAdminFrontendUrl().replaceAll("/$", "");
        return base + "/orderDetails?orderId=" + orderId;
    }

    private boolean isPaymentPaid(String paymentStatus) {
        if (paymentStatus == null) {
            return false;
        }
        String normalized = paymentStatus.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("paid")
                || normalized.contains("success")
                || normalized.contains("captured");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Orders store UTC wall-clock in LocalDateTime (same as user-service).
     * Return ISO-8601 with {@code Z} so admin clients parse as UTC then show local/IST correctly.
     */
    private String toUtcIso(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC).toString();
    }

    /** Pre-formatted India time for UIs that display the string directly. */
    private String formatOrderDateTimeIst(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(DISPLAY_ZONE)
                .format(DISPLAY_DATE_TIME)
                .replace("AM", "am")
                .replace("PM", "pm");
    }
}
