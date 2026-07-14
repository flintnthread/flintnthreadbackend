package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.AdminSetting;
import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.OrderItem;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerPayoutRequest;
import com.ecommerce.adminbackend.repository.AdminSettingRepository;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerPayoutRequestRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.PayoutAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutAdminServiceImpl extends BaseAdminService implements PayoutAdminService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SellerRepository sellerRepository;
    private final SellerPayoutRequestRepository payoutRequestRepository;
    private final AdminSettingRepository adminSettingRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private static final String KEY_B2C = "commission_b2c";
    private static final String KEY_B2B = "commission_b2b";
    private static final String DEFAULT_B2C = "15";
    private static final String DEFAULT_B2B = "7";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listPayouts(String status, int page, int size) {
        var result = orderRepository.findSellerPayments(blankToNull(status), PageRequest.of(page, size));
        List<Long> orderIds = result.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return PageResponse.from(result.map(order -> toPayoutSummary(order, itemsByOrder.getOrDefault(order.getId(), List.of()))));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> payoutStats() {
        long pending = orderRepository.countBySellerPaymentStatusIgnoreCase("pending");
        long paid = orderRepository.countBySellerPaymentStatusIgnoreCase("paid");
        long cancelled = orderRepository.countBySellerPaymentStatusIgnoreCase("cancelled");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", pending + paid + cancelled);
        stats.put("pending", pending);
        stats.put("paid", paid);
        stats.put("cancelled", cancelled);
        stats.put("totalPaidAmount", orderRepository.sumPaidSellerPaymentAmount());
        stats.put("greenCount", orderRepository.countPendingSellerPaymentsWithinDays(2));
        stats.put("orangeCount", orderRepository.countPendingSellerPaymentsDaysBetween(3, 4));
        stats.put("redCount", orderRepository.countPendingSellerPaymentsAtLeastDays(5));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPayoutDetail(Long id) {
        Order order = requireFound(orderRepository.findById(id), "Seller payment order not found.");
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return buildPayoutDetail(order, items, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateInvoice(Long id) {
        Map<String, Object> detail = getPayoutDetail(id);
        detail.put("invoiceNumber", "INV-" + id + "-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()));
        detail.put("invoiceDate", LocalDateTime.now());
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPayoutsCsv(String status, Integer minReminderDays) {
        var orders = orderRepository.findSellerPayments(blankToNull(status), PageRequest.of(0, 10_000)).getContent();
        if (minReminderDays != null && minReminderDays > 0) {
            orders = orders.stream()
                    .filter(order -> computeReminderDays(order) >= minReminderDays)
                    .toList();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, SellerPayoutRequest> payoutByOrder = orderIds.isEmpty()
                ? Map.of()
                : payoutRequestRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.toMap(SellerPayoutRequest::getOrderId, request -> request, (a, b) -> a));

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(csvHeader(
                "Order ID", "Order Number", "Order Date", "Order Status",
                "Seller Name", "Seller Email", "Seller Phone",
                "Customer Name", "Customer Email",
                "Customer Paid", "Order Amount", "GST Amount", "Delivery Charge",
                "Commission Rate", "Commission Amount", "Final Payable",
                "Delivery Date", "Reminder Days", "Reminder Bucket",
                "Payment Status", "Transaction Ref", "Admin Note", "Seller Note",
                "Wallet Balance", "Bank Name", "Account Number", "IFSC Code", "Account Holder"
        )).append('\n');

        for (Order order : orders) {
            Map<String, Object> row = toPayoutSummary(order, itemsByOrder.getOrDefault(order.getId(), List.of()));
            Map<String, Object> breakdown = (Map<String, Object>) row.get("amountBreakdown");
            SellerPayoutRequest payoutRequest = payoutByOrder.get(order.getId());
            int reminderDays = computeReminderDays(order);
            String paymentStatus = row.get("status") != null ? String.valueOf(row.get("status")) : "";
            String reminderBucket = reminderBucket(reminderDays, paymentStatus);

            csv.append(csvEscape(row.get("orderId"))).append(',');
            csv.append(csvEscape(row.get("orderNumber"))).append(',');
            csv.append(csvEscape(row.get("requestedAt"))).append(',');
            csv.append(csvEscape(row.get("orderStatus"))).append(',');
            csv.append(csvEscape(row.get("sellerName"))).append(',');
            csv.append(csvEscape(row.get("sellerEmail"))).append(',');
            csv.append(csvEscape(row.get("sellerPhone"))).append(',');
            csv.append(csvEscape(row.get("customerName"))).append(',');
            csv.append(csvEscape(row.get("customerEmail"))).append(',');
            csv.append(csvEscape(row.get("customerPaidAmount"))).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("orderAmount") : null)).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("gstAmount") : null)).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("deliveryCharge") : null)).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("commissionRate") : null)).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("commissionAmount") : null)).append(',');
            csv.append(csvEscape(breakdown != null ? breakdown.get("finalPayableAmount") : row.get("requestedAmount"))).append(',');
            csv.append(csvEscape(row.get("deliveryDate"))).append(',');
            csv.append(csvEscape(reminderDays)).append(',');
            csv.append(csvEscape(reminderBucket)).append(',');
            csv.append(csvEscape(paymentStatus)).append(',');
            csv.append(csvEscape(payoutRequest != null ? payoutRequest.getTransactionRef() : null)).append(',');
            csv.append(csvEscape(payoutRequest != null ? payoutRequest.getAdminNote() : null)).append(',');
            csv.append(csvEscape(payoutRequest != null ? payoutRequest.getSellerNote() : null)).append(',');
            csv.append(csvEscape(row.get("walletBalance"))).append(',');
            csv.append(csvEscape(row.get("bankName"))).append(',');
            csv.append(csvEscape(row.get("accountNumber"))).append(',');
            csv.append(csvEscape(row.get("ifscCode"))).append(',');
            csv.append(csvEscape(row.get("accountHolderName"))).append('\n');
        }
        return csv.toString();
    }

    private int computeReminderDays(Order order) {
        String payStatus = order.getSellerPaymentStatus();
        if (payStatus != null
                && ("paid".equalsIgnoreCase(payStatus) || "cancelled".equalsIgnoreCase(payStatus))) {
            return 0;
        }
        LocalDateTime reference = isDelivered(order) ? order.getUpdatedAt() : order.getCreatedAt();
        if (reference == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(reference.toLocalDate(), LocalDateTime.now().toLocalDate());
    }

    private String reminderBucket(int days, String paymentStatus) {
        if (paymentStatus != null
                && ("paid".equalsIgnoreCase(paymentStatus) || "cancelled".equalsIgnoreCase(paymentStatus))) {
            return "green";
        }
        if (days >= 5) {
            return "red";
        }
        if (days >= 3) {
            return "orange";
        }
        return "green";
    }

    @Override
    @Transactional
    public Map<String, Object> markPaid(Long id, String transactionRef, String adminNote, String status) {
        Order order = requireFound(orderRepository.findById(id), "Seller payment order not found.");
        String targetStatus = status != null && !status.isBlank() ? status.trim().toLowerCase() : "paid";
        if ("paid".equalsIgnoreCase(order.getSellerPaymentStatus())) {
            throw new IllegalArgumentException("Seller payment is already marked as paid.");
        }
        if ("cancelled".equalsIgnoreCase(order.getSellerPaymentStatus())) {
            throw new IllegalArgumentException("Cannot update a cancelled seller payment.");
        }

        if ("cancelled".equals(targetStatus)) {
            order.setSellerPaymentStatus("cancelled");
        } else {
            order.setSellerPaymentStatus("paid");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        payoutRequestRepository.findByOrderId(order.getId()).ifPresent(request -> {
            request.setStatus(targetStatus);
            request.setTransactionRef(transactionRef != null ? transactionRef.trim() : null);
            request.setAdminNote(adminNote != null ? adminNote.trim() : null);
            request.setPaidAt("paid".equals(targetStatus) ? LocalDateTime.now() : null);
            request.setReviewedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            payoutRequestRepository.save(request);
        });

        return Map.of(
                "payoutId", id,
                "orderId", id,
                "status", targetStatus,
                "message", "paid".equals(targetStatus) ? "Seller payment marked as paid." : "Seller payment cancelled.",
                "transactionRef", transactionRef != null ? transactionRef.trim() : "",
                "adminNote", adminNote != null ? adminNote.trim() : ""
        );
    }

    private Map<String, Object> toPayoutSummary(Order order, List<OrderItem> items) {
        return buildPayoutDetail(order, items, true);
    }

    private Map<String, Object> buildPayoutDetail(Order order, List<OrderItem> items, boolean summaryOnly) {
        OrderItem primaryItem = items.stream()
                .filter(item -> item.getSellerId() != null && item.getSellerId() > 0)
                .findFirst()
                .orElse(items.isEmpty() ? null : items.get(0));

        Long sellerId = primaryItem != null ? primaryItem.getSellerId() : null;
        Seller seller = sellerId != null ? sellerRepository.findById(sellerId).orElse(null) : null;
        Optional<SellerPayoutRequest> payoutRequest = payoutRequestRepository.findByOrderId(order.getId());

        BigDecimal orderLinesAmount = items.stream()
                .map(this::itemTotal)
                .filter(total -> total.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (orderLinesAmount.compareTo(BigDecimal.ZERO) <= 0 && order.getTotalAmount() != null) {
            orderLinesAmount = order.getTotalAmount();
        }

        BigDecimal gstAmount = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal deliveryCharge = order.getShippingAmount() != null ? order.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal commissionRate = resolveCommissionRate(seller);
        BigDecimal commissionAmount = orderLinesAmount
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalPayable = orderLinesAmount
                .subtract(gstAmount)
                .subtract(deliveryCharge)
                .subtract(commissionAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        String sellerName = primaryItem != null && primaryItem.getSellerName() != null && !primaryItem.getSellerName().isBlank()
                ? primaryItem.getSellerName()
                : seller != null ? seller.getFullName() : null;

        LocalDateTime deliveryAt = isDelivered(order) ? order.getUpdatedAt() : null;

        Map<String, Object> amountBreakdown = new LinkedHashMap<>();
        amountBreakdown.put("orderAmount", orderLinesAmount);
        amountBreakdown.put("gstAmount", gstAmount);
        amountBreakdown.put("deliveryCharge", deliveryCharge);
        amountBreakdown.put("deliveryType", "Intra-City");
        amountBreakdown.put("commissionRate", commissionRate);
        amountBreakdown.put("commissionAmount", commissionAmount);
        amountBreakdown.put("finalPayableAmount", finalPayable);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", order.getId());
        detail.put("sellerId", sellerId);
        detail.put("sellerName", sellerName);
        detail.put("sellerEmail", seller != null ? seller.getEmail() : null);
        detail.put("sellerPhone", seller != null ? seller.getMobile() : null);
        detail.put("orderId", order.getId());
        detail.put("orderNumber", order.getOrderNumber());
        detail.put("orderStatus", order.getOrderStatus());
        detail.put("requestedAmount", finalPayable);
        detail.put("customerPaidAmount", order.getTotalAmount());
        detail.put("customerName", order.getShippingName());
        detail.put("customerEmail", order.getShippingEmail());
        detail.put("status", order.getSellerPaymentStatus());
        detail.put("walletBalance", seller != null ? seller.getWalletBalance() : BigDecimal.ZERO);
        detail.put("requestedAt", order.getCreatedAt());
        detail.put("deliveryDate", deliveryAt);
        detail.put("paidAt", "paid".equalsIgnoreCase(order.getSellerPaymentStatus()) ? order.getUpdatedAt() : null);
        detail.put("transactionRef", payoutRequest.map(SellerPayoutRequest::getTransactionRef).orElse(null));
        detail.put("adminNote", payoutRequest.map(SellerPayoutRequest::getAdminNote).orElse(null));
        detail.put("sellerNote", payoutRequest.map(SellerPayoutRequest::getSellerNote).orElse(null));
        detail.put("amountBreakdown", amountBreakdown);
        detail.put("bankName", seller != null ? seller.getBankName() : null);
        detail.put("branchName", seller != null ? seller.getBranchName() : null);
        detail.put("accountNumber", seller != null ? seller.getAccountNumber() : null);
        detail.put("ifscCode", seller != null ? seller.getIfscCode() : null);
        detail.put("accountHolderName", seller != null ? seller.getAccountHolder() : null);

        if (!summaryOnly) {
            detail.put("shippingAddress1", order.getShippingAddress1());
            detail.put("shippingAddress2", order.getShippingAddress2());
            detail.put("shippingCity", order.getShippingCity());
            detail.put("shippingState", order.getShippingState());
            detail.put("shippingPincode", order.getShippingPincode());
            detail.put("shippingPhone", order.getShippingPhone());
            detail.put("sellerGstin", seller != null ? seller.getGstNumber() : null);
            detail.put("sellerAddress", seller != null ? seller.getAddress() : null);
            detail.put("sellerCity", seller != null ? seller.getCity() : null);
            detail.put("sellerState", seller != null ? seller.getState() : null);
            detail.put("sellerPincode", seller != null ? seller.getPincode() : null);
            Map<Long, Product> productsById = loadProducts(items);
            Map<Long, ProductVariant> variantsById = loadVariants(items);
            detail.put("items", items.stream()
                    .map(item -> toItemRow(item, productsById, variantsById))
                    .toList());
        }
        return detail;
    }

    private Map<String, Object> toItemRow(
            OrderItem item,
            Map<Long, Product> productsById,
            Map<Long, ProductVariant> variantsById) {
        Product product = item.getProductId() != null ? productsById.get(item.getProductId()) : null;
        ProductVariant variant = item.getVariantId() != null ? variantsById.get(item.getVariantId()) : null;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productId", item.getProductId());
        row.put("variantId", item.getVariantId());
        row.put("productName", resolveProductName(item, product));
        row.put("sku", resolveSku(item, product, variant));
        row.put("hsnCode", resolveHsnCode(item, product));
        row.put("color", blankToEmpty(item.getColor()));
        row.put("size", blankToEmpty(item.getSize()));
        row.put("quantity", item.getQuantity());
        row.put("price", item.getPrice());
        row.put("total", itemTotal(item));
        return row;
    }

    private Map<Long, Product> loadProducts(List<OrderItem> items) {
        Set<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Product> byId = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            byId.put(product.getId(), product);
        }
        return byId;
    }

    private Map<Long, ProductVariant> loadVariants(List<OrderItem> items) {
        Set<Long> variantIds = items.stream()
                .map(OrderItem::getVariantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProductVariant> byId = new HashMap<>();
        for (ProductVariant variant : productVariantRepository.findAllById(variantIds)) {
            byId.put(variant.getId(), variant);
        }
        return byId;
    }

    private String resolveProductName(OrderItem item, Product product) {
        if (!isBlank(item.getProductName())) {
            return item.getProductName().trim();
        }
        if (product != null && !isBlank(product.getName())) {
            return product.getName().trim();
        }
        return "";
    }

    private String resolveSku(OrderItem item, Product product, ProductVariant variant) {
        if (!isBlank(item.getSku())) {
            return item.getSku().trim();
        }
        if (variant != null && !isBlank(variant.getSku())) {
            return variant.getSku().trim();
        }
        if (product != null && !isBlank(product.getSku())) {
            return product.getSku().trim();
        }
        return "";
    }

    private String resolveHsnCode(OrderItem item, Product product) {
        if (!isBlank(item.getHsnCode())) {
            return item.getHsnCode().trim();
        }
        if (product != null && !isBlank(product.getHsnCode())) {
            return product.getHsnCode().trim();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private BigDecimal resolveCommissionRate(Seller seller) {
        String key = seller != null && seller.getSellerCategory() != null
                && "b2b".equalsIgnoreCase(seller.getSellerCategory().name())
                ? KEY_B2B : KEY_B2C;
        String defaultValue = KEY_B2B.equals(key) ? DEFAULT_B2B : DEFAULT_B2C;
        return adminSettingRepository.findBySettingKey(key)
                .map(AdminSetting::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> new BigDecimal(value.trim()))
                .orElse(new BigDecimal(defaultValue));
    }

    private BigDecimal itemTotal(OrderItem item) {
        if (item.getTotal() != null) {
            return item.getTotal();
        }
        if (item.getPrice() != null && item.getQuantity() != null) {
            return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        }
        return BigDecimal.ZERO;
    }

    private boolean isDelivered(Order order) {
        return order.getOrderStatus() != null
                && "delivered".equalsIgnoreCase(order.getOrderStatus().trim());
    }
}
