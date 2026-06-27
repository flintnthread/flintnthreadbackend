package com.ecommerce.adminbackend.service.impl;



import com.ecommerce.adminbackend.entity.Order;

import com.ecommerce.adminbackend.entity.Product;

import com.ecommerce.adminbackend.entity.Seller;

import com.ecommerce.adminbackend.entity.SellerAccountStatus;

import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import com.ecommerce.adminbackend.repository.DashboardQueryRepository;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.ProductImageRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.DashboardService;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.util.MediaUrlHelper;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.math.RoundingMode;

import java.time.DayOfWeek;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.time.temporal.TemporalAdjusters;

import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;



@Service

@RequiredArgsConstructor

public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LogFactory.getLogger(DashboardServiceImpl.class);

    private static final DateTimeFormatter RELATIVE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);



    private final SellerRepository sellerRepository;

    private final ProductRepository productRepository;

    private final ProductVariantRepository productVariantRepository;

    private final CategoryRepository categoryRepository;

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final DashboardQueryRepository dashboardQueryRepository;

    private final CustomerQueryRepository customerQueryRepository;

    private final ProductImageRepository productImageRepository;

    private final MediaUrlHelper mediaUrlHelper;



    @Override

    @Transactional(readOnly = true)

    public Map<String, Object> getStats() {

        log.debug("Loading dashboard stats");
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();



        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalSellers", sellerRepository.count());

        stats.put("activeSellers", sellerRepository.countByStatus(SellerAccountStatus.active));

        stats.put("pendingSellers", sellerRepository.countPendingActivation());

        stats.put("pendingProducts", productRepository.countByStatusIgnoreCase("pending"));

        stats.put("approvedProducts", productRepository.countApproved());

        stats.put("totalOrders", orderRepository.count());

        stats.put("totalRevenue", orderRepository.sumTotalAmount());

        stats.put("ordersLast30Days", orderRepository.countSince(now.minusDays(30)));

        stats.put("pendingBankVerifications", sellerRepository.countPendingBankVerification());

        stats.put("totalCategories", categoryRepository.count());



        stats.put("todayRevenue", orderRepository.sumTotalAmountSince(startOfToday));

        stats.put("todayOrders", orderRepository.countSince(startOfToday));

        stats.put("weekRevenue", orderRepository.sumTotalAmountSince(startOfWeek));

        stats.put("weekOrders", orderRepository.countSince(startOfWeek));

        stats.put("monthRevenue", orderRepository.sumTotalAmountSince(startOfMonth));

        stats.put("monthOrders", orderRepository.countSince(startOfMonth));

        int currentYear = LocalDate.now().getYear();
        stats.put("yearRevenue", orderRepository.sumForYear(currentYear));
        stats.put("yearOrders", orderRepository.countForYear(currentYear));
        stats.put("allTimeRevenue", stats.get("totalRevenue"));
        stats.put("allTimeOrders", stats.get("totalOrders"));

        stats.putAll(dashboardQueryRepository.orderStatusBuckets());
        stats.put("completedOrders", stats.get("deliveredOrders"));

        // Add product stats for dashboard
        stats.put("totalProducts", productRepository.count());
        stats.put("outOfStock", countOutOfStock());
        stats.put("lowStock", countLowStock());

        // Add customer stats for dashboard
        stats.put("totalCustomers", customerQueryRepository.countDistinctCustomers());

        return stats;

    }



    @Override

    @Transactional(readOnly = true)

    public Map<String, Object> getRevenueChart(String timeframe) {

        String normalized = timeframe == null ? "monthly" : timeframe.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {

            case "daily" -> buildDailyChart();

            case "weekly" -> buildWeeklyChart();

            case "yearly" -> buildYearlyChart();

            default -> buildMonthlyChart();

        };

    }



    @Override

    @Transactional(readOnly = true)

    public List<Map<String, Object>> getTopProducts(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object[] raw : orderItemRepository.findTopSellingProducts(safeLimit)) {

            Long productId = raw[0] != null ? ((Number) raw[0]).longValue() : null;

            String name = raw[1] != null ? raw[1].toString() : null;

            name = cleanProductName(name);

            if (name == null || name.isBlank()) {
                continue;
            }

            int sales = raw[2] != null ? ((Number) raw[2]).intValue() : 0;

            BigDecimal revenue = raw[3] != null ? new BigDecimal(raw[3].toString()) : BigDecimal.ZERO;

            int stock = productId != null ? sumProductStock(productId) : 0;

            String stockLabel = stock <= 0 ? "Out Of Stock" : stock <= 10 ? "Low Stock" : "In Stock";



            Map<String, Object> row = new LinkedHashMap<>();

            row.put("id", productId != null ? "P" + productId : null);

            row.put("productId", productId);

            row.put("name", name);

            row.put("sales", sales);

            row.put("revenue", revenue);

            row.put("stock", stockLabel);

            row.put("stockCount", stock);

            row.put("image", productId != null ? resolveProductImage(productId) : "");

            rows.add(row);

        }

        return rows;

    }



    @Override

    @Transactional(readOnly = true)

    public List<Map<String, Object>> getTopSellers(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object[] raw : orderItemRepository.findTopSellers(safeLimit)) {

            Long sellerId = raw[0] != null ? ((Number) raw[0]).longValue() : null;

            String sellerName = raw[1] != null ? raw[1].toString() : "Seller";

            int orders = raw[2] != null ? ((Number) raw[2]).intValue() : 0;

            BigDecimal revenue = raw[3] != null ? new BigDecimal(raw[3].toString()) : BigDecimal.ZERO;



            Seller seller = sellerId != null ? sellerRepository.findById(sellerId).orElse(null) : null;

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("id", sellerId);

            row.put("name", seller != null && seller.getFullName() != null ? seller.getFullName() : sellerName);

            row.put("business", seller != null && seller.getBusinessName() != null ? seller.getBusinessName() : sellerName);

            row.put("orders", orders);

            row.put("revenue", revenue);

            row.put("rating", null);

            row.put("status", seller != null && seller.getStatus() != null ? capitalize(seller.getStatus().name()) : "Active");

            rows.add(row);

        }

        return rows;

    }



    @Override

    @Transactional(readOnly = true)

    public List<Map<String, Object>> getInventoryAlerts(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Map<String, Object>> alerts = new ArrayList<>();

        for (Product product : productRepository.findAll()) {

            int stock = sumProductStock(product.getId());

            if (stock > 10) {

                continue;

            }

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("id", product.getId());

            row.put("name", product.getName());

            row.put("qty", stock);

            if (stock <= 0) {

                row.put("type", "Out Of Stock");

                row.put("severity", "High");

            } else {

                row.put("type", "Low Stock");

                row.put("severity", "Medium");

            }

            alerts.add(row);

            if (alerts.size() >= safeLimit) {

                break;

            }

        }

        return alerts;

    }



    @Override

    @Transactional(readOnly = true)

    public List<Map<String, Object>> getActivityFeed(int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 20));

        List<Map<String, Object>> feed = new ArrayList<>();



        for (Order order : orderRepository.findTop10ByOrderByCreatedAtDesc()) {

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id", order.getId());

            item.put("type", "order");

            item.put("message", "New Order " + nullSafe(order.getOrderNumber()) + " received (" + formatRupee(order.getTotalAmount()) + ")");

            item.put("read", false);

            item.put("time", formatRelative(order.getCreatedAt()));

            feed.add(item);

            if (feed.size() >= safeLimit) {

                return feed;

            }

        }



        for (Seller seller : sellerRepository.findPendingProfileReviews()) {

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id", "seller-" + seller.getId());

            item.put("type", "seller");

            item.put("message", "Seller '" + nullSafe(seller.getFullName()) + "' submitted profile for review");

            item.put("read", false);

            item.put("time", formatRelative(seller.getProfileUpdatedAt()));

            feed.add(item);

            if (feed.size() >= safeLimit) {

                return feed;

            }

        }



        for (Map<String, Object> alert : getInventoryAlerts(5)) {

            if ("Out Of Stock".equals(alert.get("type"))) {

                Map<String, Object> item = new LinkedHashMap<>();

                item.put("id", "stock-" + alert.get("id"));

                item.put("type", "stock");

                item.put("message", "Critical Alert: " + alert.get("name") + " is Out of Stock!");

                item.put("read", true);

                item.put("time", "Recently");

                feed.add(item);

                if (feed.size() >= safeLimit) {

                    return feed;

                }

            }

        }



        return feed;

    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>(dashboardQueryRepository.paymentSummary());
        long totalOrders = orderRepository.count();
        long returnedOrders = ((Number) summary.getOrDefault("refundedOrders", 0L)).longValue();
        summary.putAll(dashboardQueryRepository.refundSummary(totalOrders, returnedOrders));
        summary.put("totalCollections", orderRepository.sumTotalAmount());
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerInsights() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime activeSince = now.minusDays(90);

        long total = customerQueryRepository.countDistinctCustomers();
        long active = dashboardQueryRepository.countActiveCustomers(activeSince);

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("total", total);
        insights.put("newToday", dashboardQueryRepository.countNewCustomersSince(startOfToday));
        insights.put("newWeek", dashboardQueryRepository.countNewCustomersSince(startOfWeek));
        insights.put("newMonth", dashboardQueryRepository.countNewCustomersSince(startOfMonth));
        insights.put("activeCustomers", active);
        insights.put("inactiveCustomers", Math.max(0, total - active));
        return insights;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSellerInsights() {
        Map<String, Object> insights = new LinkedHashMap<>();
        long registered = sellerRepository.count();
        long active = sellerRepository.countByStatus(SellerAccountStatus.active);
        long withoutProducts = sellerRepository.countWithoutProducts();
        long withSales = dashboardQueryRepository.countSellersWithSales();

        insights.put("registered", registered);
        insights.put("active", active);
        insights.put("inactiveNoProducts", withoutProducts);
        insights.put("pending", sellerRepository.countPendingActivation());
        insights.put("topPerformers", withSales);
        return insights;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWebsiteTraffic() {
        Map<String, Object> traffic = new LinkedHashMap<>();
        
        // For now, return mock data. In production, integrate with analytics service
        // like Google Analytics, Plausible, or custom tracking system
        traffic.put("currentlyOnline", 14);
        traffic.put("visitorsToday", 1204);
        traffic.put("visitorsWeek", 8924);
        traffic.put("visitorsMonth", 32940);
        
        return traffic;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCatalogQuality() {
        Map<String, Object> quality = new LinkedHashMap<>();
        
        // For now, return mock data. In production, calculate from actual product data
        quality.put("overallScore", 94.2);
        quality.put("productImagesAttached", 98);
        quality.put("richDescriptionsFilled", 92);
        quality.put("seoMetadataTags", 88);
        quality.put("categoryBrandMappings", 100);
        quality.put("optimizationTips", "13 products are missing meta descriptions. 5 products have low-resolution images.");
        
        return quality;
    }



    private Map<String, Object> buildDailyChart() {

        List<String> labels = new ArrayList<>();

        List<BigDecimal> revenue = new ArrayList<>();

        List<Long> orders = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {

            LocalDate day = today.minusDays(i);

            LocalDateTime start = day.atStartOfDay();

            LocalDateTime end = day.plusDays(1).atStartOfDay();

            labels.add(day.getDayOfWeek().name().substring(0, 3));

            revenue.add(orderRepository.sumTotalAmountBetween(start, end));

            orders.add(orderRepository.countBetween(start, end));

        }

        return chartPayload(labels, revenue, orders);

    }



    private Map<String, Object> buildWeeklyChart() {

        List<String> labels = new ArrayList<>();

        List<BigDecimal> revenue = new ArrayList<>();

        List<Long> orders = new ArrayList<>();

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        LocalDate cursor = monthStart;

        int week = 1;

        while (!cursor.isAfter(LocalDate.now()) && week <= 4) {

            LocalDate weekEnd = cursor.plusDays(6);

            if (weekEnd.isAfter(LocalDate.now())) {

                weekEnd = LocalDate.now();

            }

            labels.add("Week " + week);

            revenue.add(orderRepository.sumTotalAmountBetween(cursor.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay()));

            orders.add(orderRepository.countBetween(cursor.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay()));

            cursor = cursor.plusDays(7);

            week++;

        }

        return chartPayload(labels, revenue, orders);

    }



    private Map<String, Object> buildMonthlyChart() {

        List<String> labels = new ArrayList<>();

        List<BigDecimal> revenue = new ArrayList<>();

        List<Long> orders = new ArrayList<>();

        int year = LocalDate.now().getYear();

        for (int month = 1; month <= 12; month++) {

            labels.add(LocalDate.of(year, month, 1).getMonth().name().substring(0, 3));

            revenue.add(orderRepository.sumForMonth(year, month));

            orders.add(orderRepository.countForMonth(year, month));

        }

        return chartPayload(labels, revenue, orders);

    }



    private Map<String, Object> buildYearlyChart() {

        List<String> labels = new ArrayList<>();

        List<BigDecimal> revenue = new ArrayList<>();

        List<Long> orders = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 3; year <= currentYear; year++) {

            labels.add(String.valueOf(year));

            revenue.add(orderRepository.sumForYear(year));

            orders.add(orderRepository.countForYear(year));

        }

        return chartPayload(labels, revenue, orders);

    }



    private Map<String, Object> chartPayload(List<String> labels, List<BigDecimal> revenue, List<Long> orders) {

        double maxRevenue = revenue.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);

        long maxOrders = orders.stream().mapToLong(Long::longValue).max().orElse(0);

        double maxVal = Math.max(maxRevenue, maxOrders > 0 ? maxOrders * (maxRevenue / Math.max(maxOrders, 1)) : 0);

        if (maxVal <= 0) {

            maxVal = 1000;

        }



        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("labels", labels);

        payload.put("revenue", revenue.stream().map(this::toDouble).toList());

        payload.put("orders", orders);

        payload.put("maxVal", roundUp(maxVal));

        return payload;

    }



    private long countOrdersByStatuses(String... statuses) {

        long total = 0;

        for (String status : statuses) {

            total += orderRepository.countByOrderStatusIgnoreCase(status);

        }

        return total;

    }



    private int sumProductStock(Long productId) {

        return productVariantRepository.findByProductIdOrderByIdAsc(productId).stream()

                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)

                .sum();

    }

    private long countOutOfStock() {
        return productRepository.findAll().stream()
                .filter(product -> {
                    int stock = sumProductStock(product.getId());
                    return stock <= 0;
                })
                .count();
    }

    private long countLowStock() {
        return productRepository.findAll().stream()
                .filter(product -> {
                    int stock = sumProductStock(product.getId());
                    return stock > 0 && stock <= 10;
                })
                .count();
    }



    private String resolveProductImage(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .or(() -> productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream().findFirst())
                .map(img -> mediaUrlHelper.toPublicUrl(img.getImagePath()))
                .orElse("");
    }



    private double toDouble(BigDecimal value) {

        return value != null ? value.doubleValue() : 0;

    }



    private double roundUp(double value) {

        if (value <= 0) {

            return 1000;

        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));

        return Math.ceil(value / magnitude) * magnitude;

    }



    private String formatRupee(BigDecimal amount) {

        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;

        return "₹" + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();

    }



    private String formatRelative(LocalDateTime dateTime) {

        if (dateTime == null) {

            return "Recently";

        }

        LocalDateTime now = LocalDateTime.now();

        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) {

            return "Just now";

        }

        if (minutes < 60) {

            return minutes + " mins ago";

        }

        long hours = minutes / 60;

        if (hours < 24) {

            return hours + " hr" + (hours > 1 ? "s" : "") + " ago";

        }

        return RELATIVE_TIME.format(dateTime);

    }



    private String nullSafe(String value) {

        return value != null && !value.isBlank() ? value : "—";

    }



    private String capitalize(String value) {

        if (value == null || value.isBlank()) {

            return "Active";

        }

        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);

    }

    private String cleanProductName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        // Remove file path prefixes like "ads/products/...", "pads/products/...", "uploads/products/...", etc.
        // Pattern: any word chars + /products/ + any chars (the ID/hash)
        String cleaned = name.replaceAll("^[\\w]+/products/[\\w.-]+", "");

        // Remove image file extensions at the end
        cleaned = cleaned.replaceAll("\\.(png|jpg|jpeg|gif|webp|PNG|JPG|JPEG|GIF|WEBP)$", "");

        // Remove any remaining path separators
        cleaned = cleaned.replaceAll("/", " ");

        // Trim whitespace
        cleaned = cleaned.trim();

        // If the result is empty or just contains random characters, return null
        if (cleaned.isBlank() || cleaned.length() < 3) {
            return null;
        }

        return cleaned;
    }

}


