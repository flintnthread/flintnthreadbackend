package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.BankEditApprovalRequest;
import com.ecommerce.sellerbackend.dto.BankEditRequest;
import com.ecommerce.sellerbackend.dto.BankEditResponse;
import com.ecommerce.sellerbackend.dto.financial.DashboardOverviewDto;
import com.ecommerce.sellerbackend.dto.financial.DashboardPeriodStatsDto;
import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.entity.Order;
import com.ecommerce.sellerbackend.entity.OrderItem;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerBankEditRequest;
import com.ecommerce.sellerbackend.entity.SellerPayoutRequest;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.OrderRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.SellerBankEditRequestRepository;
import com.ecommerce.sellerbackend.repository.SellerPayoutRequestRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.SellerFinancialService;
import com.ecommerce.sellerbackend.service.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerFinancialServiceImpl implements SellerFinancialService {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final SellerPayoutRequestRepository payoutRequestRepository;
    private final SellerBankEditRequestRepository bankEditRequestRepository;
    private final ShiprocketService shiprocketService;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Override
    public Map<String, Object> getDashboard(Long sellerId) {
        SellerContext ctx = loadContext(sellerId);
        PeriodWindow allTime = periodWindow("year", null, null);
        PeriodMetrics metrics = aggregate(ctx, allTime);

        Map<String, Long> statusCounts = countByUiStatus(ctx);
        List<Map<String, Object>> topProducts = getTopProducts(sellerId, 5);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overview", DashboardOverviewDto.builder()
                .orders(metrics.distinctOrders())
                .sales(metrics.sales())
                .views(0)
                .rating(0)
                .reviewCount(0)
                .salesFormatted(formatInr(metrics.sales()))
                .build());
        response.put("orderSummary", Map.of(
                "pending", statusCounts.getOrDefault("Pending", 0L).intValue(),
                "processing", statusCounts.getOrDefault("Processing", 0L).intValue(),
                "shipped", statusCounts.getOrDefault("Shipped", 0L).intValue(),
                "delivered", statusCounts.getOrDefault("Delivered", 0L).intValue(),
                "returns", statusCounts.getOrDefault("Returned", 0L).intValue()
        ));
        response.put("topProducts", topProducts);
        response.put("totalProducts", productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).size());
        return response;
    }

    @Override
    public Map<String, Object> getCharts(Long sellerId, String period) {
        SellerContext ctx = loadContext(sellerId);
        PeriodWindow window = periodWindow(period, null, null);
        List<ChartBucket> buckets = buildBuckets(window);
        List<Map<String, Object>> salesPoints = new ArrayList<>();
        List<Map<String, Object>> ordersPoints = new ArrayList<>();
        List<Map<String, Object>> productsPoints = new ArrayList<>();
        double totalSales = 0;
        int totalOrders = 0;
        int totalUnits = 0;

        for (ChartBucket bucket : buckets) {
            PeriodMetrics m = aggregate(ctx, bucket.window());
            salesPoints.add(Map.of("label", bucket.label(), "value", m.sales()));
            ordersPoints.add(Map.of("label", bucket.label(), "value", m.distinctOrders()));
            productsPoints.add(Map.of("label", bucket.label(), "value", m.unitsSold()));
            totalSales += m.sales();
            totalOrders += m.distinctOrders();
            totalUnits += m.unitsSold();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("period", normalizePeriod(period));
        response.put("salesPoints", salesPoints);
        response.put("ordersPoints", ordersPoints);
        response.put("productsPoints", productsPoints);
        response.put("totalSales", totalSales);
        response.put("totalOrders", totalOrders);
        response.put("totalUnitsSold", totalUnits);
        response.put("totalSalesFormatted", formatInr(totalSales));
        return response;
    }

    @Override
    public Map<String, DashboardPeriodStatsDto> getStatsByPeriod(Long sellerId) {
        SellerContext ctx = loadContext(sellerId);
        Map<String, DashboardPeriodStatsDto> out = new LinkedHashMap<>();
        for (String period : List.of("day", "week", "month", "year")) {
            PeriodMetrics m = aggregate(ctx, periodWindow(period, null, null));
            out.put(period, DashboardPeriodStatsDto.builder()
                    .period(period)
                    .orders(m.distinctOrders())
                    .sales(m.sales())
                    .salesFormatted(formatInr(m.sales()))
                    .views(0)
                    .rating(0)
                    .returns(m.returns())
                    .newCustomers(m.newCustomers())
                    .conversionRate(0)
                    .build());
        }
        return out;
    }

    @Override
    public Map<String, Object> getAnalyticsSales(Long sellerId, String period) {
        SellerContext ctx = loadContext(sellerId);
        PeriodMetrics m = aggregate(ctx, periodWindow(period, null, null));
        List<Map<String, Object>> channels = buildChannels(ctx, periodWindow(period, null, null));
        return Map.of(
                "period", normalizePeriod(period),
                "totalSales", m.sales(),
                "totalOrders", m.distinctOrders(),
                "salesFormatted", formatInr(m.sales()),
                "channels", channels
        );
    }

    @Override
    public List<Map<String, Object>> getTopProducts(Long sellerId, int limit) {
        SellerContext ctx = loadContext(sellerId);
        Map<Long, Integer> soldByProduct = new HashMap<>();
        Map<Long, String> imageByProduct = new HashMap<>();

        for (OrderItem item : ctx.items()) {
            if (!isEligibleSale(item, ctx.order(item.getOrderId()))) continue;
            if (item.getProductId() == null) continue;
            soldByProduct.merge(item.getProductId(), safeQty(item), Integer::sum);
            if (item.getProductImagePath() != null) {
                imageByProduct.putIfAbsent(item.getProductId(), resolveImage(item.getProductImagePath()));
            }
        }

        List<Long> productIds = soldByProduct.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long productId : productIds) {
            Product product = products.get(productId);
            int sold = soldByProduct.getOrDefault(productId, 0);
            rows.add(Map.of(
                    "id", String.valueOf(productId),
                    "name", product != null ? nullToEmpty(product.getName()) : nullToEmpty(
                            ctx.items().stream()
                                    .filter(i -> productId.equals(i.getProductId()))
                                    .map(OrderItem::getProductName)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse("Product")),
                    "price", formatInr(ctx.items().stream()
                            .filter(i -> productId.equals(i.getProductId()))
                            .mapToDouble(this::itemTotal)
                            .findFirst()
                            .orElse(0)),
                    "sold", sold,
                    "image", imageByProduct.getOrDefault(productId, ""),
                    "category", ""
            ));
        }
        return rows;
    }

    @Override
    public Map<String, Object> getAnalyticsOverview(Long sellerId, String period, String channel) {
        SellerContext ctx = loadContext(sellerId);
        PeriodWindow window = periodWindow(period, null, null);
        PeriodMetrics m = aggregate(ctx, window);
        List<Map<String, Object>> channels = buildChannels(ctx, window);
        List<Map<String, Object>> paymentMethods = getPaymentMethods(sellerId, period);
        double aov = m.distinctOrders() > 0 ? m.sales() / m.distinctOrders() : 0;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", m.sales());
        out.put("orders", m.distinctOrders());
        out.put("aov", round2(aov));
        out.put("returns", m.returns());
        out.put("cancels", m.cancellations());
        out.put("replacements", 0);
        out.put("channels", channels);
        out.put("paymentMethods", paymentMethods);
        return out;
    }

    @Override
    public List<Map<String, Object>> getSalesTrend(Long sellerId, String period, String from, String to) {
        SellerContext ctx = loadContext(sellerId);
        PeriodWindow window = periodWindow(period, from, to);
        List<ChartBucket> buckets = buildBuckets(window);
        List<Map<String, Object>> points = new ArrayList<>();
        for (ChartBucket bucket : buckets) {
            PeriodMetrics m = aggregate(ctx, bucket.window());
            points.add(Map.of("label", bucket.label(), "value", m.sales()));
        }
        return points;
    }

    @Override
    public List<Map<String, Object>> getPaymentMethods(Long sellerId, String period) {
        SellerContext ctx = loadContext(sellerId);
        PeriodWindow window = periodWindow(period, null, null);
        Map<String, Double> amountByMethod = new LinkedHashMap<>();
        Map<String, Integer> ordersByMethod = new LinkedHashMap<>();

        Set<Long> seenOrders = new java.util.HashSet<>();
        for (OrderItem item : ctx.itemsInWindow(window)) {
            Order order = ctx.order(item.getOrderId());
            if (!isEligibleSale(item, order)) continue;
            String method = formatPaymentMethod(order != null ? order.getPaymentMethod() : null);
            amountByMethod.merge(method, itemTotal(item), Double::sum);
            if (seenOrders.add(item.getOrderId())) {
                ordersByMethod.merge(method, 1, Integer::sum);
            }
        }

        double total = amountByMethod.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Double> entry : amountByMethod.entrySet()) {
            double amount = entry.getValue();
            int orders = ordersByMethod.getOrDefault(entry.getKey(), 0);
            double pct = total > 0 ? (amount / total) * 100 : 0;
            rows.add(Map.of(
                    "label", entry.getKey(),
                    "value", round2(amount),
                    "pct", round2(pct),
                    "orders", orders
            ));
        }
        return rows;
    }

    @Override
    public Map<String, Object> getEarnings(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        SellerContext ctx = loadContext(sellerId);
        double pending = computePendingBalance(sellerId, ctx);
        List<Map<String, Object>> transactions = buildWalletTransactions(sellerId);

        Map<String, Object> bank = null;
        if (seller.getBankName() != null && !seller.getBankName().isBlank()) {
            bank = Map.of(
                    "bankName", seller.getBankName(),
                    "accountNumberMasked", maskAccount(seller.getAccountNumber()),
                    "ifscCode", nullToEmpty(seller.getIfscCode()),
                    "accountHolder", nullToEmpty(seller.getAccountHolder()),
                    "verified", seller.getBankProof() != null && !seller.getBankProof().isBlank()
            );
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("availableBalance", round2(pending));
        out.put("totalCredits", round2(computeLifetimePaid(ctx));
        out.put("totalDebits", round2(sumPaidOutPayouts(sellerId)));
        out.put("transactions", transactions);
        if (bank != null) out.put("bankAccount", bank);
        return out;
    }

    @Override
    public List<Map<String, Object>> getEarningsPayouts(Long sellerId) {
        return loadPayoutRequests(sellerId).stream()
                .map(this::toPayoutTransaction)
                .toList();
    }

    @Override
    public Map<String, Object> lookupOrderPayout(Long sellerId, String orderKey) {
        SellerContext ctx = loadContext(sellerId);
        Optional<Long> orderId = resolveOrderId(ctx, orderKey);
        if (orderId.isEmpty()) {
            return Map.of("orderKey", orderKey, "amount", 0, "found", false);
        }
        double amount = ctx.items().stream()
                .filter(i -> orderId.get().equals(i.getOrderId()))
                .filter(i -> isEligibleSale(i, ctx.order(i.getOrderId())))
                .mapToDouble(this::itemTotal)
                .sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderKey", orderKey);
        out.put("orderId", orderId.get());
        out.put("amount", round2(amount));
        out.put("found", amount > 0);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> requestEarningsPayout(Long sellerId, Map<String, Object> body) {
        double amount = toDouble(body.get("amount"));
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid payout amount.");
        }
        SellerContext ctx = loadContext(sellerId);
        double available = computePendingBalance(sellerId, ctx);
        if (amount > available) {
            throw new IllegalArgumentException("Insufficient balance.");
        }

        String orderKey = body.get("orderId") != null ? String.valueOf(body.get("orderId")).trim() : "";
        Long orderId = 0L;
        if (!orderKey.isBlank()) {
            orderId = resolveOrderId(ctx, orderKey).orElseThrow(
                    () -> new IllegalArgumentException("Order not found: " + orderKey));
        }

        SellerPayoutRequest row = new SellerPayoutRequest();
        row.setSellerId(sellerId);
        row.setOrderId(orderId);
        row.setRequestedAmount(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
        row.setStatus("pending");
        row.setSellerNote(body.get("description") != null ? String.valueOf(body.get("description")) : null);
        row.setRequestedAt(LocalDateTime.now());
        payoutRequestRepository.save(row);

        double remaining = available - amount;
        return Map.of(
                "transactionId", String.valueOf(row.getId()),
                "amount", amount,
                "remainingBalance", round2(Math.max(remaining, 0)),
                "status", "pending",
                "message", "Payout request submitted successfully."
        );
    }

    @Override
    public Map<String, Object> getPayoutSummary(Long sellerId) {
        requireSeller(sellerId);
        SellerContext ctx = loadContext(sellerId);
        double lifetime = computeLifetimeEligible(ctx);
        double thisMonth = aggregate(ctx, periodWindow("month", null, null)).sales();
        double pending = computePendingBalance(sellerId, ctx);
        double highest = listPayoutRequests(sellerId).stream()
                .mapToDouble(this::safePayoutAmount)
                .max()
                .orElse(0);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sellerId", sellerId);
        out.put("lifetimeEarnings", round2(lifetime));
        out.put("thisMonthEarnings", round2(thisMonth));
        out.put("highestPayout", round2(highest));
        out.put("pendingAmount", round2(pending));
        return out;
    }

    @Override
    public Map<String, Object> getBankDetails(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sellerId", sellerId);
        out.put("bankName", nullToEmpty(seller.getBankName()));
        out.put("branchName", nullToEmpty(seller.getBranchName()));
        out.put("accountHolder", nullToEmpty(seller.getAccountHolder()));
        out.put("accountNumber", nullToEmpty(seller.getAccountNumber()));
        out.put("ifscCode", nullToEmpty(seller.getIfscCode()));
        out.put("bankProof", seller.getBankProof());
        out.put("cancelledCheque", seller.getCancelledCheque());
        out.put("bankVerified", seller.getBankProof() != null && !seller.getBankProof().isBlank());
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> updateBankDetails(Long sellerId, Map<String, String> body) {
        Seller seller = requireSeller(sellerId);
        if (body.get("bankName") != null) seller.setBankName(body.get("bankName").trim());
        if (body.get("branchName") != null) seller.setBranchName(body.get("branchName").trim());
        if (body.get("accountHolder") != null) seller.setAccountHolder(body.get("accountHolder").trim());
        if (body.get("accountNumber") != null) seller.setAccountNumber(body.get("accountNumber").trim());
        if (body.get("ifscCode") != null) seller.setIfscCode(body.get("ifscCode").trim().toUpperCase(Locale.ROOT));
        sellerRepository.save(seller);
        return getBankDetails(sellerId);
    }

    @Override
    public String confirmBankDetails(Long sellerId, String note) {
        requireSeller(sellerId);
        return "Bank details confirmed.";
    }

    @Override
    @Transactional
    public SellerPayoutRequest submitPayoutRequest(Long sellerId, String orderId, String sellerNote) {
        SellerContext ctx = loadContext(sellerId);
        Long resolvedOrderId = resolveOrderId(ctx, orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        double amount = ctx.items().stream()
                .filter(i -> resolvedOrderId.equals(i.getOrderId()))
                .filter(i -> isEligibleSale(i, ctx.order(i.getOrderId())))
                .mapToDouble(this::itemTotal)
                .sum();
        if (amount <= 0) {
            throw new IllegalArgumentException("No eligible payout amount for this order.");
        }

        SellerPayoutRequest row = new SellerPayoutRequest();
        row.setSellerId(sellerId);
        row.setOrderId(resolvedOrderId);
        row.setRequestedAmount(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
        row.setStatus("pending");
        row.setSellerNote(sellerNote);
        row.setRequestedAt(LocalDateTime.now());
        return payoutRequestRepository.save(row);
    }

    @Override
    public List<SellerPayoutRequest> listPayoutRequests(Long sellerId) {
        return loadPayoutRequests(sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPayoutRequestsCsv(Long sellerId) {
        List<SellerPayoutRequest> rows = loadPayoutRequests(sellerId);
        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Order ID,Amount (INR),Requested At,Status,Transaction Ref,Paid At,Seller Note,Admin Note\n");
        for (SellerPayoutRequest row : rows) {
            csv.append(csvEscape(row.getId())).append(',');
            csv.append(csvEscape(row.getOrderId())).append(',');
            csv.append(csvEscape(row.getRequestedAmount())).append(',');
            csv.append(csvEscape(row.getRequestedAt())).append(',');
            csv.append(csvEscape(row.getStatus())).append(',');
            csv.append(csvEscape(row.getTransactionRef())).append(',');
            csv.append(csvEscape(row.getPaidAt())).append(',');
            csv.append(csvEscape(row.getSellerNote())).append(',');
            csv.append(csvEscape(row.getAdminNote())).append('\n');
        }
        return csv.toString();
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    @Override
    @Transactional
    public ShiprocketSyncResponse syncShiprocket(Long sellerId, String orderKey) {
        SellerContext ctx = loadContext(sellerId);
        Long orderId = resolveOrderId(ctx, orderKey)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderKey));
        List<OrderItem> sellerItems = ctx.items().stream()
                .filter(i -> orderId.equals(i.getOrderId()))
                .toList();
        if (sellerItems.isEmpty()) {
            throw new ResourceNotFoundException("Order not found for seller: " + orderKey);
        }
        Order order = ctx.order(orderId);
        if (order == null) {
            order = buildSyntheticOrder(orderId, sellerItems);
        }
        return shiprocketService.syncTracking(order);
    }

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

    private SellerContext loadContext(Long sellerId) {
        List<OrderItem> items = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        Set<Long> orderIds = items.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Order> orders = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Order order : orderRepository.findByIdIn(orderIds)) {
                if (order.getId() != null) {
                    orders.put(order.getId(), order);
                }
            }
        }
        return new SellerContext(items, orders);
    }

    private List<SellerPayoutRequest> loadPayoutRequests(Long sellerId) {
        try {
            return payoutRequestRepository.findBySellerIdOrderByRequestedAtDesc(sellerId);
        } catch (Exception ex) {
            log.warn("Could not load payout requests for seller {}: {}", sellerId, ex.getMessage());
            return List.of();
        }
    }

    private double safePayoutAmount(SellerPayoutRequest row) {
        if (row == null || row.getRequestedAmount() == null) {
            return 0;
        }
        return row.getRequestedAmount().doubleValue();
    }

    private PeriodMetrics aggregate(SellerContext ctx, PeriodWindow window) {
        double sales = 0;
        int units = 0;
        Set<Long> orders = new java.util.HashSet<>();
        Set<Long> customers = new java.util.HashSet<>();
        int returns = 0;
        int cancellations = 0;

        for (OrderItem item : ctx.itemsInWindow(window)) {
            Order order = ctx.order(item.getOrderId());
            String uiStatus = toUiStatus(item.getStatus(), order);
            if ("Returned".equals(uiStatus)) returns++;
            if ("Cancelled".equals(uiStatus)) cancellations++;
            if (!isEligibleSale(item, order)) continue;
            sales += itemTotal(item);
            units += safeQty(item);
            orders.add(item.getOrderId());
            if (order != null && order.getUserId() != null) {
                customers.add(order.getUserId());
            }
        }

        return new PeriodMetrics(round2(sales), orders.size(), units, customers.size(), returns, cancellations);
    }

    private Map<String, Long> countByUiStatus(SellerContext ctx) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Set<Long> seen = new java.util.HashSet<>();
        for (OrderItem item : ctx.items()) {
            if (!seen.add(item.getOrderId())) continue;
            Order order = ctx.order(item.getOrderId());
            String status = toUiStatus(resolveOrderStatus(order, item), order);
            counts.merge(status, 1L, Long::sum);
        }
        return counts;
    }

    private List<Map<String, Object>> buildChannels(SellerContext ctx, PeriodWindow window) {
        Map<String, Double> amount = new LinkedHashMap<>();
        Map<String, Integer> orders = new LinkedHashMap<>();
        Set<String> seen = new java.util.HashSet<>();
        for (OrderItem item : ctx.itemsInWindow(window)) {
            Order order = ctx.order(item.getOrderId());
            if (!isEligibleSale(item, order)) continue;
            String channel = formatPaymentMethod(order != null ? order.getPaymentMethod() : null);
            amount.merge(channel, itemTotal(item), Double::sum);
            String key = channel + ":" + item.getOrderId();
            if (seen.add(key)) orders.merge(channel, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Double> entry : amount.entrySet()) {
            rows.add(Map.of(
                    "name", entry.getKey(),
                    "amount", round2(entry.getValue()),
                    "orders", orders.getOrDefault(entry.getKey(), 0)
            ));
        }
        return rows;
    }

    private double computePendingBalance(Long sellerId, SellerContext ctx) {
        double eligible = 0;
        for (OrderItem item : ctx.items()) {
            Order order = ctx.order(item.getOrderId());
            if (!isEligibleSale(item, order)) continue;
            if (!isSellerPaymentPending(order)) continue;
            eligible += itemTotal(item);
        }
        double reserved = loadPayoutRequests(sellerId)
                .stream()
                .filter(p -> "pending".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(this::safePayoutAmount)
                .sum();
        return Math.max(eligible - reserved, 0);
    }

    private double computeLifetimeEligible(SellerContext ctx) {
        return ctx.items().stream()
                .filter(i -> isEligibleSale(i, ctx.order(i.getOrderId())))
                .mapToDouble(this::itemTotal)
                .sum();
    }

    private double computeLifetimePaid(SellerContext ctx) {
        return ctx.items().stream()
                .filter(i -> isEligibleSale(i, ctx.order(i.getOrderId())))
                .filter(i -> !isSellerPaymentPending(ctx.order(i.getOrderId())))
                .mapToDouble(this::itemTotal)
                .sum();
    }

    private double sumPaidOutPayouts(Long sellerId) {
        return loadPayoutRequests(sellerId).stream()
                .filter(p -> "paid".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(this::safePayoutAmount)
                .sum();
    }

    private List<Map<String, Object>> buildWalletTransactions(Long sellerId) {
        return loadPayoutRequests(sellerId).stream()
                .limit(20)
                .map(this::toWalletTransaction)
                .toList();
    }

    private Map<String, Object> toWalletTransaction(SellerPayoutRequest row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("title", "Payout Request");
        out.put("amount", "-" + formatInr(safePayoutAmount(row)));
        out.put("date", row.getRequestedAt() != null ? DISPLAY_DATE.format(row.getRequestedAt()) : "");
        out.put("status", capitalize(row.getStatus()));
        out.put("type", "debit");
        if (row.getOrderId() != null && row.getOrderId() > 0) out.put("orderId", row.getOrderId());
        return out;
    }

    private Map<String, Object> toPayoutTransaction(SellerPayoutRequest row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", String.valueOf(row.getId()));
        out.put("orderId", row.getOrderId() != null ? String.valueOf(row.getOrderId()) : "");
        out.put("amount", safePayoutAmount(row));
        out.put("date", row.getRequestedAt() != null ? row.getRequestedAt().toString() : "");
        out.put("status", capitalize(row.getStatus()));
        out.put("type", "Payout");
        return out;
    }

    private Optional<Long> resolveOrderId(SellerContext ctx, String orderKey) {
        if (orderKey == null || orderKey.isBlank()) return Optional.empty();
        String key = orderKey.trim();
        if (key.startsWith("#")) {
            key = key.substring(1);
        }

        if (key.chars().allMatch(Character::isDigit)) {
            long numeric = Long.parseLong(key);
            if (ctx.items().stream().anyMatch(i -> Objects.equals(i.getOrderId(), numeric))) {
                return Optional.of(numeric);
            }
        }

        if (key.toUpperCase(Locale.ROOT).startsWith("ORD-")) {
            String suffix = key.substring(4);
            if (!suffix.isBlank() && suffix.chars().allMatch(Character::isDigit)) {
                long numeric = Long.parseLong(suffix);
                if (ctx.items().stream().anyMatch(i -> Objects.equals(i.getOrderId(), numeric))) {
                    return Optional.of(numeric);
                }
            }
        }

        for (Order order : ctx.orders().values()) {
            if (key.equalsIgnoreCase(order.getOrderNumber())) return Optional.of(order.getId());
            if (key.equals(String.valueOf(order.getId()))) return Optional.of(order.getId());
        }
        return Optional.empty();
    }

    private boolean isEligibleSale(OrderItem item, Order order) {
        if (item == null) return false;
        String itemStatus = nullToEmpty(item.getStatus()).toLowerCase(Locale.ROOT);
        if (itemStatus.contains("cancel")) return false;
        if (order != null) {
            String orderStatus = nullToEmpty(order.getOrderStatus()).toLowerCase(Locale.ROOT);
            if (orderStatus.contains("cancel")) return false;
            String pay = nullToEmpty(order.getPaymentStatus()).toLowerCase(Locale.ROOT);
            if (!pay.contains("paid") && !pay.contains("success") && !pay.contains("completed")) {
                return false;
            }
        }
        return itemTotal(item) > 0;
    }

    private boolean isSellerPaymentPending(Order order) {
        if (order == null) return true;
        String status = nullToEmpty(order.getSellerPaymentStatus()).toLowerCase(Locale.ROOT);
        return status.isBlank() || status.contains("pending");
    }

    private double itemTotal(OrderItem item) {
        if (item.getTotal() != null) return item.getTotal().doubleValue();
        if (item.getPrice() != null && item.getQuantity() != null) {
            return item.getPrice().doubleValue() * item.getQuantity();
        }
        return 0;
    }

    private int safeQty(OrderItem item) {
        return item.getQuantity() != null ? item.getQuantity() : 0;
    }

    private String resolveOrderStatus(Order order, OrderItem item) {
        if (order != null && order.getOrderStatus() != null && !order.getOrderStatus().isBlank()) {
            return order.getOrderStatus();
        }
        return item.getStatus();
    }

    private String toUiStatus(String raw, Order order) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "pending", "awaiting_payment", "awaiting_processing" -> "Pending";
            case "processing", "sent_to_seller" -> "Processing";
            case "shipped", "in_transit", "out_for_delivery" -> "Shipped";
            case "delivered", "completed" -> "Delivered";
            case "returned", "return", "refunded", "rto_initiated", "rto_delivered", "replacement" -> "Returned";
            case "cancelled", "canceled" -> "Cancelled";
            default -> value.isBlank() ? "Pending" : capitalize(value.replace('_', ' '));
        };
    }

    private PeriodWindow periodWindow(String period, String from, String to) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            start = LocalDate.parse(from).atStartOfDay();
            end = LocalDate.parse(to).atTime(LocalTime.MAX);
            return new PeriodWindow(start, end, "custom");
        }
        return switch (normalizePeriod(period)) {
            case "day" -> new PeriodWindow(end.toLocalDate().atStartOfDay(), end, "day");
            case "week" -> new PeriodWindow(end.minusDays(6).toLocalDate().atStartOfDay(), end, "week");
            case "month" -> new PeriodWindow(end.minusDays(29).toLocalDate().atStartOfDay(), end, "month");
            case "year" -> new PeriodWindow(end.minusDays(364).toLocalDate().atStartOfDay(), end, "year");
            default -> new PeriodWindow(end.minusDays(6).toLocalDate().atStartOfDay(), end, "week");
        };
    }

    private List<ChartBucket> buildBuckets(PeriodWindow window) {
        List<ChartBucket> buckets = new ArrayList<>();
        String period = window.period();
        LocalDateTime end = window.end();
        if ("day".equals(period)) {
            for (int h = 0; h < 24; h += 4) {
                LocalDateTime start = end.toLocalDate().atTime(h, 0);
                LocalDateTime bucketEnd = h == 20 ? end : end.toLocalDate().atTime(Math.min(h + 3, 23), 59);
                buckets.add(new ChartBucket(String.format("%02d:00", h), new PeriodWindow(start, bucketEnd, period)));
            }
            return buckets;
        }
        if ("week".equals(period)) {
            for (int i = 6; i >= 0; i--) {
                LocalDate day = end.toLocalDate().minusDays(i);
                buckets.add(new ChartBucket(day.getDayOfWeek().name().substring(0, 3),
                        new PeriodWindow(day.atStartOfDay(), day.atTime(LocalTime.MAX), period)));
            }
            return buckets;
        }
        if ("month".equals(period)) {
            for (int i = 3; i >= 0; i--) {
                LocalDate startDay = end.toLocalDate().minusDays(i * 7L + 6);
                LocalDate endDay = end.toLocalDate().minusDays(i * 7L);
                buckets.add(new ChartBucket("W" + (4 - i),
                        new PeriodWindow(startDay.atStartOfDay(), endDay.atTime(LocalTime.MAX), period)));
            }
            return buckets;
        }
        for (int i = 11; i >= 0; i--) {
            LocalDate month = end.toLocalDate().withDayOfMonth(1).minusMonths(i);
            LocalDateTime start = month.atStartOfDay();
            LocalDateTime bucketEnd = month.plusMonths(1).minusDays(1).atTime(LocalTime.MAX);
            buckets.add(new ChartBucket(month.getMonth().name().substring(0, 3),
                    new PeriodWindow(start, bucketEnd, period)));
        }
        return buckets;
    }

    private String normalizePeriod(String period) {
        if (period == null) return "week";
        return switch (period.trim().toLowerCase(Locale.ROOT)) {
            case "day" -> "day";
            case "week" -> "week";
            case "month" -> "month";
            case "year" -> "year";
            default -> "week";
        };
    }

    private String formatPaymentMethod(String method) {
        if (method == null || method.isBlank()) return "Online";
        String lower = method.toLowerCase(Locale.ROOT);
        if (lower.contains("cod") || lower.contains("cash")) return "COD";
        return "Online";
    }

    private String formatInr(double value) {
        return INR.format(value).replace("₹", "₹");
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private String maskAccount(String account) {
        if (account == null || account.length() < 4) return "••••";
        return "•••• " + account.substring(account.length() - 4);
    }

    private String resolveImage(String path) {
        if (path == null || path.isBlank()) return "";
        if (path.startsWith("http")) return path;
        if (mediaBaseUrl == null || mediaBaseUrl.isBlank()) return path;
        return mediaBaseUrl.replaceAll("/$", "") + "/" + path.replaceAll("^/", "");
    }

    private Seller requireSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return "";
        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }

    private record SellerContext(List<OrderItem> items, Map<Long, Order> orders) {
        Order order(Long orderId) {
            return orders.get(orderId);
        }

        List<OrderItem> itemsInWindow(PeriodWindow window) {
            return items.stream()
                    .filter(i -> {
                        LocalDateTime ts = i.getCreatedAt() != null ? i.getCreatedAt() : null;
                        if (ts == null) return true;
                        return !ts.isBefore(window.start()) && !ts.isAfter(window.end());
                    })
                    .toList();
        }
    }

    private record PeriodWindow(LocalDateTime start, LocalDateTime end, String period) {}

    private record PeriodMetrics(double sales, int distinctOrders, int unitsSold, int newCustomers, int returns, int cancellations) {}

    private record ChartBucket(String label, PeriodWindow window) {}

    @Override
    @Transactional
    public BankEditResponse submitBankEditRequest(Long sellerId, BankEditRequest request) {
        Seller seller = requireSeller(sellerId);

        // Check if there's already a pending request
        long pendingCount = bankEditRequestRepository.countBySellerIdAndStatusIgnoreCase(sellerId, "pending");
        if (pendingCount > 0) {
            throw new IllegalArgumentException("You already have a pending bank edit request. Please wait for it to be processed.");
        }

        SellerBankEditRequest editRequest = new SellerBankEditRequest();
        editRequest.setSellerId(sellerId);

        // Store old bank details
        editRequest.setOldBankName(seller.getBankName());
        editRequest.setOldAccountNumber(seller.getAccountNumber());
        editRequest.setOldIfscCode(seller.getIfscCode());
        editRequest.setOldAccountHolder(seller.getAccountHolder());
        editRequest.setOldBranchName(seller.getBranchName());

        // Store new bank details
        editRequest.setNewBankName(request.getBankName());
        editRequest.setNewAccountNumber(request.getAccountNumber());
        editRequest.setNewIfscCode(request.getIfscCode());
        editRequest.setNewAccountHolder(request.getAccountHolder());
        editRequest.setNewBranchName(request.getBranchName());

        // Store reason
        editRequest.setReason(request.getReason());
        editRequest.setStatus("pending");
        editRequest.setRequestedAt(LocalDateTime.now());

        SellerBankEditRequest saved = bankEditRequestRepository.save(editRequest);

        return toBankEditResponse(saved);
    }

    @Override
    public List<BankEditResponse> listBankEditRequests(Long sellerId) {
        return bankEditRequestRepository.findBySellerIdOrderByRequestedAtDesc(sellerId).stream()
                .map(this::toBankEditResponse)
                .toList();
    }

    @Override
    @Transactional
    public BankEditResponse approveBankEditRequest(Long requestId, BankEditApprovalRequest approvalRequest, Long adminId) {
        SellerBankEditRequest editRequest = bankEditRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank edit request not found: " + requestId));

        if (!"pending".equalsIgnoreCase(editRequest.getStatus())) {
            throw new IllegalArgumentException("This request has already been processed.");
        }

        editRequest.setReviewedAt(LocalDateTime.now());
        editRequest.setApprovedByAdminId(adminId);
        editRequest.setAdminNote(approvalRequest.getAdminNote());

        if ("approve".equalsIgnoreCase(approvalRequest.getAction())) {
            // Update seller's bank details
            Seller seller = requireSeller(editRequest.getSellerId());
            seller.setBankName(editRequest.getNewBankName());
            seller.setAccountNumber(editRequest.getNewAccountNumber());
            seller.setIfscCode(editRequest.getNewIfscCode());
            seller.setAccountHolder(editRequest.getNewAccountHolder());
            seller.setBranchName(editRequest.getNewBranchName());
            seller.setBankVerified(false); // Reset verification status
            sellerRepository.save(seller);

            editRequest.setStatus("approved");
        } else if ("reject".equalsIgnoreCase(approvalRequest.getAction())) {
            editRequest.setStatus("rejected");
        } else {
            throw new IllegalArgumentException("Invalid action. Must be 'approve' or 'reject'.");
        }

        SellerBankEditRequest saved = bankEditRequestRepository.save(editRequest);
        return toBankEditResponse(saved);
    }

    private BankEditResponse toBankEditResponse(SellerBankEditRequest request) {
        return BankEditResponse.builder()
                .id(request.getId())
                .sellerId(request.getSellerId())
                .oldBankName(request.getOldBankName())
                .oldAccountNumber(request.getOldAccountNumber())
                .oldIfscCode(request.getOldIfscCode())
                .oldAccountHolder(request.getOldAccountHolder())
                .oldBranchName(request.getOldBranchName())
                .newBankName(request.getNewBankName())
                .newAccountNumber(request.getNewAccountNumber())
                .newIfscCode(request.getNewIfscCode())
                .newAccountHolder(request.getNewAccountHolder())
                .newBranchName(request.getNewBranchName())
                .reason(request.getReason())
                .status(request.getStatus())
                .adminNote(request.getAdminNote())
                .requestedAt(request.getRequestedAt())
                .reviewedAt(request.getReviewedAt())
                .approvedByAdminId(request.getApprovedByAdminId())
                .build();
    }
}
