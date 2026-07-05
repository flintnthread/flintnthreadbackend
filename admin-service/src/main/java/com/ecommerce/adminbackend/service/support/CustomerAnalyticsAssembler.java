package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CustomerAnalyticsAssembler {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String[] MONTH_LABELS =
            {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAY_LABELS =
            {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    private final CustomerQueryRepository customerQueryRepository;

    public Map<String, Object> build(Long customerId, Object[] customerRow) {
        String email = stringAt(customerRow, 1);
        long totalOrders = longAt(customerRow, 10);
        BigDecimal lifetimeSpend = decimalAt(customerRow[11]);
        LocalDateTime firstOrderAt = toDateTime(customerRow[12]);
        LocalDateTime lastOrderAt = toDateTime(customerRow[13]);
        BigDecimal avgOrderValue = totalOrders > 0
                ? lifetimeSpend.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Long> statusCounts = loadStatusCounts(customerId);
        long delivered = statusCounts.getOrDefault("delivered", 0L)
                + statusCounts.getOrDefault("completed", 0L);
        long processing = statusCounts.getOrDefault("processing", 0L)
                + statusCounts.getOrDefault("confirmed", 0L)
                + statusCounts.getOrDefault("shipped", 0L)
                + statusCounts.getOrDefault("packed", 0L);
        long cancelled = statusCounts.getOrDefault("cancelled", 0L);
        long returned = statusCounts.getOrDefault("returned", 0L)
                + statusCounts.getOrDefault("refunded", 0L)
                + statusCounts.getOrDefault("rto", 0L);
        long replacement = statusCounts.getOrDefault("replacement", 0L);

        List<Map<String, Object>> monthlySpend = buildMonthlySeries(
                customerQueryRepository.monthlySpendingByCustomerId(customerId), true);
        List<Map<String, Object>> monthlyOrders = buildMonthlySeries(
                customerQueryRepository.monthlyOrdersByCustomerId(customerId), false);

        List<Map<String, Object>> orderStatusBreakdown = buildStatusBreakdown(
                delivered, processing, cancelled, returned, replacement);
        List<Map<String, Object>> categories = toChartPoints(
                customerQueryRepository.topCategoriesByCustomerId(customerId), 5);
        List<Map<String, Object>> brands = toChartPoints(
                customerQueryRepository.topBrandsByCustomerId(customerId), 5);
        List<Map<String, Object>> paymentMethods = toSlicePoints(
                customerQueryRepository.paymentMethodsByCustomerId(customerId));
        List<Map<String, Object>> orderFrequency = buildWeeklyFrequency(
                customerQueryRepository.weeklyOrdersByCustomerId(customerId));
        List<Map<String, Object>> purchaseTime = buildPurchaseTime(
                customerQueryRepository.purchaseHourBucketsByCustomerId(customerId));

        Map<String, Object> behaviour = buildBehaviour(
                customerId, categories, brands, purchaseTime, totalOrders);

        BigDecimal refundAmount = decimalAt(
                customerQueryRepository.refundTotalByCustomerId(customerId));
        int returnRate = pct(returned, totalOrders);
        int replacementRate = pct(replacement, totalOrders);

        Map<String, Object> paymentsData = buildPayments(customerId, paymentMethods, email);
        Map<String, Object> addressData = buildAddress(customerRow, customerId);
        Map<String, Object> reviewsData = buildReviews(email);
        Map<String, Object> supportData = buildSupport(email);
        Map<String, Object> loyaltyData = buildLoyalty(lifetimeSpend, customerId);
        List<Map<String, Object>> recentOrders = buildRecentOrders(customerId);
        List<Map<String, Object>> timeline = buildTimeline(recentOrders, paymentsData, supportData, reviewsData, customerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("customer", buildCustomer(customerRow));
        response.put("totalOrders", totalOrders);
        response.put("lifetimeSpend", lifetimeSpend);
        response.put("avgOrderValue", avgOrderValue);
        response.put("delivered", delivered);
        response.put("processing", processing);
        response.put("cancelled", cancelled);
        response.put("returned", returned);
        response.put("replacement", replacement);
        response.put("refundAmount", refundAmount);
        response.put("monthlySpend", monthlySpend);
        response.put("monthlyOrders", monthlyOrders);
        response.put("orderStatusBreakdown", orderStatusBreakdown);
        response.put("categories", categories);
        response.put("brands", brands);
        response.put("paymentMethods", paymentMethods);
        response.put("orderFrequency", orderFrequency);
        response.put("purchaseTime", purchaseTime);
        response.put("behaviour", behaviour);
        response.put("returnsData", Map.of(
                "returnRate", returnRate,
                "replacementRate", replacementRate,
                "refundAmount", refundAmount,
                "reasons", toChartPoints(customerQueryRepository.returnReasonsByCustomerId(customerId), 10)));
        response.put("paymentsData", paymentsData);
        response.put("addressData", addressData);
        response.put("reviewsData", reviewsData);
        response.put("supportData", supportData);
        response.put("loyaltyData", loyaltyData);
        response.put("riskBadges", buildRiskBadges(totalOrders, lifetimeSpend, loyaltyData, returnRate, cancelled, paymentsData));
        response.put("aiInsights", buildInsights(behaviour, paymentsData, returnRate));
        response.put("recommendedActions", defaultActions());
        response.put("recentOrders", recentOrders);
        response.put("timeline", timeline);
        response.put("customerSince", formatDate(firstOrderAt));
        response.put("lastPurchase", formatDate(lastOrderAt));
        response.put("healthScore", computeHealthScore(totalOrders, returnRate, paymentsData, lastOrderAt));
        response.put("isVip", isVipTier(stringAt(loyaltyData, "tier")));
        response.put("status", isActive(lastOrderAt) ? "Active" : "Inactive");
        response.put("trends", buildTrends(customerId));
        return response;
    }

    private Map<String, Object> buildCustomer(Object[] row) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", longAt(row, 0));
        customer.put("email", stringAt(row, 1));
        customer.put("name", stringAt(row, 2));
        customer.put("phone", stringAt(row, 3));
        customer.put("address1", stringAt(row, 4));
        customer.put("address2", stringAt(row, 5));
        customer.put("city", stringAt(row, 6));
        customer.put("state", stringAt(row, 7));
        customer.put("country", stringAt(row, 8));
        customer.put("pincode", stringAt(row, 9));
        return customer;
    }

    private Map<String, Long> loadStatusCounts(Long customerId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : customerQueryRepository.orderStatusCountsByCustomerId(customerId)) {
            String status = stringAt(row, 0).toLowerCase(Locale.ROOT);
            counts.put(status, longAt(row, 1));
        }
        return counts;
    }

    private List<Map<String, Object>> buildMonthlySeries(List<Object[]> rows, boolean amountSeries) {
        Map<Integer, Number> values = new LinkedHashMap<>();
        for (Object[] row : rows) {
            values.put(((Number) row[0]).intValue(), (Number) row[1]);
        }
        List<Map<String, Object>> series = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", MONTH_LABELS[month - 1]);
            point.put("value", values.getOrDefault(month, 0));
            series.add(point);
        }
        return series;
    }

    private List<Map<String, Object>> buildStatusBreakdown(
            long delivered, long processing, long cancelled, long returned, long replacement) {
        List<Map<String, Object>> items = new ArrayList<>();
        addBreakdown(items, "Delivered", delivered);
        addBreakdown(items, "Processing", processing);
        addBreakdown(items, "Cancelled", cancelled);
        addBreakdown(items, "Returned", returned);
        addBreakdown(items, "Replacement", replacement);
        return items;
    }

    private void addBreakdown(List<Map<String, Object>> items, String label, long value) {
        if (value <= 0) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        items.add(row);
    }

    private List<Map<String, Object>> buildWeeklyFrequency(List<Object[]> rows) {
        List<Map<String, Object>> points = new ArrayList<>();
        int index = 1;
        for (Object[] row : rows) {
            if (index > 8) {
                break;
            }
            points.add(chartPoint("W" + index, longAt(row, 1)));
            index++;
        }
        return points;
    }

    private List<Map<String, Object>> toChartPoints(List<Object[]> rows, int limit) {
        List<Map<String, Object>> points = new ArrayList<>();
        int count = 0;
        for (Object[] row : rows) {
            if (count >= limit) {
                break;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", stringAt(row, 0));
            point.put("value", longAt(row, 1));
            points.add(point);
            count++;
        }
        return points;
    }

    private List<Map<String, Object>> toSlicePoints(List<Object[]> rows) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", normalizePaymentLabel(stringAt(row, 0)));
            point.put("value", longAt(row, 1));
            points.add(point);
        }
        return points;
    }

    private List<Map<String, Object>> buildPurchaseTime(List<Object[]> rows) {
        long morning = 0;
        long afternoon = 0;
        long evening = 0;
        long night = 0;
        for (Object[] row : rows) {
            int hour = ((Number) row[0]).intValue();
            long count = longAt(row, 1);
            if (hour >= 6 && hour < 12) {
                morning += count;
            } else if (hour >= 12 && hour < 17) {
                afternoon += count;
            } else if (hour >= 17 && hour < 21) {
                evening += count;
            } else {
                night += count;
            }
        }
        List<Map<String, Object>> buckets = new ArrayList<>();
        buckets.add(chartPoint("Morning", morning));
        buckets.add(chartPoint("Afternoon", afternoon));
        buckets.add(chartPoint("Evening", evening));
        buckets.add(chartPoint("Night", night));
        return buckets;
    }

    private Map<String, Object> chartPoint(String label, long value) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("label", label);
        point.put("value", value);
        return point;
    }

    private Map<String, Object> buildBehaviour(
            Long customerId,
            List<Map<String, Object>> categories,
            List<Map<String, Object>> brands,
            List<Map<String, Object>> purchaseTime,
            long totalOrders) {
        String topCategory = categories.isEmpty() ? "—" : stringAt(categories.get(0), "label");
        String topBrand = brands.isEmpty() ? "—" : stringAt(brands.get(0), "label");
        BigDecimal avgBasket = decimalAt(customerQueryRepository.avgBasketSizeByCustomerId(customerId));
        String activeDay = resolveActiveDay(customerId);
        String activeTime = resolveActiveTime(purchaseTime);
        String avgDelivery = formatDays(decimalAt(customerQueryRepository.avgDeliveryDaysByCustomerId(customerId)));
        String longestGap = formatDays(decimalAt(customerQueryRepository.longestOrderGapDaysByCustomerId(customerId)));
        String streak = customerQueryRepository.currentOrderStreakByCustomerId(customerId) + " orders";

        Map<String, Object> behaviour = new LinkedHashMap<>();
        behaviour.put("mostPurchasedCategory", topCategory);
        behaviour.put("favouriteBrand", topBrand);
        behaviour.put("avgBasketSize", avgBasket.setScale(1, RoundingMode.HALF_UP) + " items");
        behaviour.put("mostActiveDay", activeDay);
        behaviour.put("mostActiveTime", activeTime);
        behaviour.put("avgDeliveryTime", avgDelivery);
        behaviour.put("longestGap", longestGap);
        behaviour.put("currentStreak", streak);
        return behaviour;
    }

    private String resolveActiveDay(Long customerId) {
        List<Object[]> rows = customerQueryRepository.ordersByWeekdayByCustomerId(customerId);
        if (rows.isEmpty()) {
            return "—";
        }
        int dayIndex = ((Number) rows.get(0)[0]).intValue();
        int mysqlDay = dayIndex % 7;
        return DAY_LABELS[mysqlDay];
    }

    private String resolveActiveTime(List<Map<String, Object>> purchaseTime) {
        return purchaseTime.stream()
                .max(Comparator.comparingLong(p -> longAt(p, "value")))
                .map(p -> stringAt(p, "label"))
                .orElse("—");
    }

    private Map<String, Object> buildPayments(
            Long customerId, List<Map<String, Object>> paymentMethods, String email) {
        long totalOrders = customerQueryRepository.countOrdersForCustomer(customerId);
        long paidOrders = customerQueryRepository.paidOrdersCountByCustomerId(customerId);
        long failedPayments = customerQueryRepository.failedPaymentsCountByCustomerId(customerId);
        int successRate = totalOrders > 0 ? (int) Math.round((paidOrders * 100.0) / totalOrders) : 0;
        String preferred = paymentMethods.isEmpty() ? "—" : stringAt(paymentMethods.get(0), "label");

        List<Map<String, Object>> refundHistory = new ArrayList<>();
        for (Object[] row : customerQueryRepository.refundHistoryByCustomerId(customerId)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", formatDate(toDateTime(row[0])));
            entry.put("amount", decimalAt(row[1]));
            entry.put("reason", stringAt(row, 2));
            refundHistory.add(entry);
        }

        Map<String, Object> payments = new LinkedHashMap<>();
        payments.put("successRate", successRate);
        payments.put("preferredMethod", preferred);
        payments.put("failedPayments", failedPayments);
        payments.put("refundHistory", refundHistory);
        return payments;
    }

    private Map<String, Object> buildAddress(Object[] customerRow, Long customerId) {
        String primary = String.join(", ",
                stringAt(customerRow, 4),
                stringAt(customerRow, 6),
                stringAt(customerRow, 7)).replaceAll("(^, |, $)", "").trim();
        if (primary.isBlank()) {
            primary = "—";
        }
        List<String> cities = new ArrayList<>();
        for (Object[] row : customerQueryRepository.distinctCitiesByCustomerId(customerId)) {
            cities.add(stringAt(row, 0));
        }
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("primary", primary);
        address.put("savedCount", Math.max(1, cities.size()));
        address.put("mostDelivered", cities.isEmpty() ? stringAt(customerRow, 6) : cities.get(0));
        address.put("recentLocations", cities.stream().limit(3).toList());
        return address;
    }

    private Map<String, Object> buildReviews(String email) {
        Object[] summary = customerQueryRepository.reviewSummaryByEmail(email).orElse(null);
        Map<String, Object> reviews = new LinkedHashMap<>();
        if (summary == null) {
            reviews.put("submitted", 0);
            reviews.put("avgRating", "0.0");
            reviews.put("photoReviews", 0);
            reviews.put("helpfulVotes", 0);
            return reviews;
        }
        reviews.put("submitted", longAt(summary, 0));
        reviews.put("avgRating", decimalAt(summary[1]).setScale(1, RoundingMode.HALF_UP).toPlainString());
        reviews.put("photoReviews", longAt(summary, 2));
        reviews.put("helpfulVotes", 0);
        return reviews;
    }

    private Map<String, Object> buildSupport(String email) {
        Object[] summary = customerQueryRepository.supportSummaryByEmail(email).orElse(null);
        Map<String, Object> support = new LinkedHashMap<>();
        if (summary == null) {
            support.put("tickets", 0);
            support.put("resolved", 0);
            support.put("pending", 0);
            support.put("avgResolutionTime", "—");
            support.put("latestTicket", "No tickets raised");
            return support;
        }
        long total = longAt(summary, 0);
        long resolved = longAt(summary, 1);
        support.put("tickets", total);
        support.put("resolved", resolved);
        support.put("pending", Math.max(0, total - resolved));
        support.put("avgResolutionTime", summary[2] != null ? formatHours(decimalAt(summary[2])) : "—");
        support.put("latestTicket", stringAt(summary, 3));
        return support;
    }

    private Map<String, Object> buildLoyalty(BigDecimal lifetimeSpend, Long customerId) {
        String tier = resolveTier(lifetimeSpend);
        BigDecimal savings = decimalAt(customerQueryRepository.totalSavingsByCustomerId(customerId));
        long couponsUsed = customerQueryRepository.couponUsageCountByCustomerId(customerId);
        int points = lifetimeSpend.intValue() / 100;

        Map<String, Object> loyalty = new LinkedHashMap<>();
        loyalty.put("tier", tier);
        loyalty.put("points", points);
        loyalty.put("progressPct", tierProgress(lifetimeSpend));
        loyalty.put("nextTier", nextTier(tier));
        loyalty.put("couponsUsed", couponsUsed);
        loyalty.put("lifetimeSavings", savings);
        return loyalty;
    }

    private List<Map<String, Object>> buildRecentOrders(Long customerId) {
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Object[] row : customerQueryRepository.recentOrdersWithProductByCustomerId(customerId)) {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("id", String.valueOf(longAt(row, 0)));
            order.put("orderId", longAt(row, 0));
            order.put("orderNumber", stringAt(row, 1));
            String productName = stringAt(row, 2);
            order.put("productName", productName.isBlank() ? "Product" : productName);
            order.put("date", formatDate(toDateTime(row[3])));
            order.put("amount", decimalAt(row[4]));
            order.put("status", normalizeUiStatus(stringAt(row, 5)));
            order.put("payment", normalizePaymentLabel(stringAt(row, 6)));
            orders.add(order);
        }
        return orders;
    }

    private List<Map<String, Object>> buildTimeline(
            List<Map<String, Object>> recentOrders,
            Map<String, Object> paymentsData,
            Map<String, Object> supportData,
            Map<String, Object> reviewsData,
            Long customerId) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (Map<String, Object> order : recentOrders) {
            events.add(timelineEvent("order", "Order placed — #" + stringAt(order, "orderNumber"), stringAt(order, "date")));
        }
        for (Object[] row : customerQueryRepository.refundHistoryByCustomerId(customerId)) {
            events.add(timelineEvent("return", "Refund processed — " + stringAt(row, 2), formatDate(toDateTime(row[0]))));
        }
        if (longAt(supportData, "tickets") > 0) {
            events.add(timelineEvent("ticket", "Support ticket: " + stringAt(supportData, "latestTicket"), "Recent"));
        }
        if (longAt(reviewsData, "submitted") > 0) {
            events.add(timelineEvent(
                    "review",
                    "Submitted a " + stringAt(reviewsData, "avgRating") + "★ review",
                    "Recent"));
        }
        return events.stream().limit(8).toList();
    }

    private Map<String, Object> timelineEvent(String type, String title, String date) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("title", title);
        event.put("date", date);
        return event;
    }

    private List<Map<String, Object>> buildRiskBadges(
            long totalOrders,
            BigDecimal lifetimeSpend,
            Map<String, Object> loyaltyData,
            int returnRate,
            long cancelled,
            Map<String, Object> paymentsData) {
        List<Map<String, Object>> badges = new ArrayList<>();
        if (lifetimeSpend.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            badges.add(badge("High Value Customer"));
        }
        if (totalOrders > 5) {
            badges.add(badge("Repeat Buyer"));
        }
        if (isVipTier(stringAt(loyaltyData, "tier"))) {
            badges.add(badge("VIP Customer"));
        }
        if (returnRate < 10) {
            badges.add(badge("Low Return Rate"));
        }
        if (totalOrders > 0 && ((double) cancelled / totalOrders) > 0.15) {
            badges.add(badge("Frequent Canceller"));
        }
        if (longAt(paymentsData, "failedPayments") > 2) {
            badges.add(badge("Late Payment Risk"));
        }
        if (badges.isEmpty()) {
            badges.add(badge("Standard Customer"));
        }
        return badges;
    }

    private Map<String, Object> badge(String label) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        return row;
    }

    private List<String> buildInsights(Map<String, Object> behaviour, Map<String, Object> paymentsData, int returnRate) {
        List<String> insights = new ArrayList<>();
        insights.add("Mostly shops on " + stringAt(behaviour, "mostActiveDay") + "s during "
                + stringAt(behaviour, "mostActiveTime").toLowerCase(Locale.ROOT) + ".");
        insights.add("Prefers " + stringAt(behaviour, "mostPurchasedCategory") + " over other categories.");
        insights.add("Uses " + stringAt(paymentsData, "preferredMethod") + " for the majority of purchases.");
        insights.add("Return rate is " + (returnRate < 10 ? "below" : "above") + " average for this segment.");
        return insights;
    }

    private List<Map<String, Object>> defaultActions() {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(action("Send Coupon", "gift"));
        actions.add(action("Upgrade Loyalty Tier", "crown"));
        actions.add(action("Offer Free Shipping", "truck"));
        actions.add(action("Notify About Sale", "send"));
        actions.add(action("Contact Customer", "phone"));
        actions.add(action("Flag Customer", "flag"));
        return actions;
    }

    private Map<String, Object> action(String label, String icon) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("icon", icon);
        return row;
    }

    private Map<String, Object> buildTrends(Long customerId) {
        BigDecimal recentSpend = decimalAt(customerQueryRepository.spendInLastDays(customerId, 30));
        BigDecimal priorSpend = decimalAt(customerQueryRepository.spendBetweenDays(customerId, 60, 30));
        long recentOrders = customerQueryRepository.ordersInLastDays(customerId, 30);
        long priorOrders = customerQueryRepository.ordersBetweenDays(customerId, 60, 30);

        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("spendTrend", trendLabel(recentSpend, priorSpend));
        trends.put("ordersTrend", trendLabel(BigDecimal.valueOf(recentOrders), BigDecimal.valueOf(priorOrders)));
        return trends;
    }

    private String trendLabel(BigDecimal recent, BigDecimal prior) {
        if (prior.compareTo(BigDecimal.ZERO) <= 0) {
            return recent.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal change = recent.subtract(prior)
                .multiply(BigDecimal.valueOf(100))
                .divide(prior, 0, RoundingMode.HALF_UP);
        return (change.signum() >= 0 ? "+" : "") + change + "%";
    }

    private int computeHealthScore(long totalOrders, int returnRate, Map<String, Object> paymentsData, LocalDateTime lastOrderAt) {
        int score = 60;
        score += Math.min((int) totalOrders * 2, 20);
        score += longAt(paymentsData, "successRate") >= 90 ? 10 : 0;
        score -= Math.min(returnRate, 20);
        if (lastOrderAt != null && ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), LocalDate.now()) <= 30) {
            score += 10;
        }
        return Math.max(55, Math.min(98, score));
    }

    private boolean isActive(LocalDateTime lastOrderAt) {
        return lastOrderAt != null && ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), LocalDate.now()) <= 90;
    }

    private boolean isVipTier(String tier) {
        return "Gold".equalsIgnoreCase(tier) || "Platinum".equalsIgnoreCase(tier);
    }

    private String resolveTier(BigDecimal lifetimeSpend) {
        double spend = lifetimeSpend.doubleValue();
        if (spend >= 100000) return "Platinum";
        if (spend >= 50000) return "Gold";
        if (spend >= 10000) return "Silver";
        return "Bronze";
    }

    private int tierProgress(BigDecimal lifetimeSpend) {
        double spend = lifetimeSpend.doubleValue();
        if (spend >= 100000) return 100;
        if (spend >= 50000) return (int) Math.round(((spend - 50000) / 50000) * 100);
        if (spend >= 10000) return (int) Math.round(((spend - 10000) / 40000) * 100);
        return (int) Math.round((spend / 10000) * 100);
    }

    private String nextTier(String tier) {
        return switch (tier) {
            case "Bronze" -> "Silver";
            case "Silver" -> "Gold";
            case "Gold" -> "Platinum";
            default -> "Platinum";
        };
    }

    private String normalizeUiStatus(String status) {
        String value = status == null ? "" : status.toLowerCase(Locale.ROOT);
        if (value.contains("deliver") || value.contains("complete")) return "Delivered";
        if (value.contains("cancel")) return "Cancelled";
        if (value.contains("return") || value.contains("refund") || value.contains("rto")) return "Returned";
        if (value.contains("replace")) return "Replacement";
        if (value.contains("ship") || value.contains("process") || value.contains("confirm") || value.contains("pack")) {
            return "Processing";
        }
        return "Pending";
    }

    private String normalizePaymentLabel(String method) {
        if (method == null || method.isBlank()) return "—";
        String value = method.toLowerCase(Locale.ROOT);
        if (value.contains("upi")) return "UPI";
        if (value.contains("card")) return "Card";
        if (value.contains("cod") || value.contains("cash")) return "COD";
        if (value.contains("wallet")) return "Wallet";
        if (value.contains("online")) return "Online Payment";
        return method.substring(0, 1).toUpperCase(Locale.ROOT) + method.substring(1);
    }

    private int pct(long part, long total) {
        if (total <= 0) return 0;
        return (int) Math.round((part * 100.0) / total);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_DATE);
    }

    private String formatDays(BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }
        return days.setScale(0, RoundingMode.HALF_UP) + " days";
    }

    private String formatHours(BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }
        return hours.setScale(0, RoundingMode.HALF_UP) + "h";
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime dateTime) return dateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }

    private String stringAt(Object[] row, int index) {
        return row == null || row.length <= index || row[index] == null ? "" : String.valueOf(row[index]).trim();
    }

    private String stringAt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private long longAt(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) return 0L;
        return ((Number) row[index]).longValue();
    }

    private long longAt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.longValue();
        if (value == null) return 0L;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private BigDecimal decimalAt(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
