package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.AnalyticsChannelDto;
import com.ecommerce.sellerbackend.dto.AnalyticsOverviewDto;
import com.ecommerce.sellerbackend.dto.AnalyticsSalesResponse;
import com.ecommerce.sellerbackend.dto.PaymentMethodBreakdownDto;
import com.ecommerce.sellerbackend.dto.SalesTrendPointDto;
import com.ecommerce.sellerbackend.dto.TopSellingProductResponse;
import com.ecommerce.sellerbackend.util.AnalyticsPeriodUtil;
import com.ecommerce.sellerbackend.entity.Category;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.repository.CategoryRepository;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductReviewRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSalesResponse getSales(Long sellerId, String period) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        BigDecimal totalSales = orderItemRepository.sumSalesBetween(sellerId, range[0], range[1]);
        long totalOrders = orderItemRepository.countDistinctOrdersBetween(sellerId, range[0], range[1]);

        List<AnalyticsChannelDto> channels = List.of(
                AnalyticsChannelDto.builder().name("Delivered").amount(totalSales).orders(totalOrders).build()
        );

        return AnalyticsSalesResponse.builder()
                .period(period != null ? period : "month")
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .salesFormatted(INR.format(totalSales != null ? totalSales : BigDecimal.ZERO))
                .channels(channels)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopProducts(Long sellerId, int limit) {
        List<Object[]> rows = orderItemRepository.topProductIdsByQuantity(sellerId);
        if (rows.isEmpty()) {
            return List.of();
        }

        int cap = Math.max(1, Math.min(limit, 50));
        List<Long> productIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .limit(cap)
                .toList();

        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, List<ProductVariant>> variants = productVariantRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));
        Map<Long, List<ProductImage>> images = productImageRepository
                .findByProductIdInOrderByIsPrimaryDescSortOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
        Map<Integer, String> categoryNames = categoryRepository.findAllById(
                products.values().stream().map(Product::getCategoryId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Category::getId, Category::getCategoryName));

        Map<Long, Double> avgRatings = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] ratingRow : productReviewRepository.averageRatingByProductIds(productIds)) {
                Long pid = ((Number) ratingRow[0]).longValue();
                Double avg = ratingRow[1] != null ? ((Number) ratingRow[1]).doubleValue() : null;
                if (avg != null) {
                    avgRatings.put(pid, Math.round(avg * 10.0) / 10.0);
                }
            }
        }

        List<TopSellingProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            long sold = ((Number) row[1]).longValue();
            Product product = products.get(productId);
            if (product == null) {
                continue;
            }
            ProductVariant variant = variants.getOrDefault(productId, List.of()).stream()
                    .filter(v -> v.getFinalPrice() != null)
                    .findFirst()
                    .orElse(variants.getOrDefault(productId, List.of()).stream().findFirst().orElse(null));
            BigDecimal price = variant != null && variant.getFinalPrice() != null
                    ? variant.getFinalPrice()
                    : BigDecimal.ZERO;
            BigDecimal mrp = variant != null && variant.getMrpPrice() != null
                    ? variant.getMrpPrice()
                    : (variant != null ? variant.getMrpInclGst() : null);
            String discountLabel = formatDiscountLabel(variant);
            String image = images.getOrDefault(productId, List.of()).stream()
                    .findFirst()
                    .map(img -> resolveImageUrl(img.getImagePath()))
                    .orElse("");

            result.add(TopSellingProductResponse.builder()
                    .id(String.valueOf(productId))
                    .name(product.getName())
                    .price(INR.format(price))
                    .sold(sold)
                    .image(image)
                    .category(categoryNames.getOrDefault(product.getCategoryId(), "—"))
                    .status(product.getStatus())
                    .avgRating(avgRatings.get(productId))
                    .mrp(mrp != null && mrp.compareTo(BigDecimal.ZERO) > 0 ? INR.format(mrp) : null)
                    .discount(discountLabel)
                    .build());

            if (result.size() >= cap) {
                break;
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getSalesTrend(Long sellerId, String period) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        return buildSalesTrendPoints(sellerId, range, period);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getSalesTrend(Long sellerId, LocalDate from, LocalDate to) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(from, to);
        return buildSalesTrendPoints(sellerId, range, "custom");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getOrdersTrend(Long sellerId, String period) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        return buildCountTrendPoints(
                orderItemRepository.countOrdersGroupedByDay(sellerId, range[0], range[1]),
                range,
                period);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getOrdersTrend(Long sellerId, LocalDate from, LocalDate to) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(from, to);
        return buildCountTrendPoints(
                orderItemRepository.countOrdersGroupedByDay(sellerId, range[0], range[1]),
                range,
                "custom");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getProductsTrend(Long sellerId, String period) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        return buildCountTrendPoints(
                orderItemRepository.sumUnitsGroupedByDay(sellerId, range[0], range[1]),
                range,
                period);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesTrendPointDto> getProductsTrend(Long sellerId, LocalDate from, LocalDate to) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(from, to);
        return buildCountTrendPoints(
                orderItemRepository.sumUnitsGroupedByDay(sellerId, range[0], range[1]),
                range,
                "custom");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodBreakdownDto> getPaymentMethods(Long sellerId, String period) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        List<Object[]> rows = orderItemRepository.sumSalesByPaymentMethod(sellerId, range[0], range[1]);
        double total = 0;
        List<PaymentMethodBreakdownDto> items = new ArrayList<>();
        for (Object[] row : rows) {
            double amt = row[1] != null ? new BigDecimal(row[1].toString()).doubleValue() : 0;
            long orders = row[2] != null ? ((Number) row[2]).longValue() : 0;
            total += amt;
            items.add(PaymentMethodBreakdownDto.builder()
                    .label(formatPaymentLabel(String.valueOf(row[0])))
                    .value(amt)
                    .orders(orders)
                    .pct(0)
                    .build());
        }
        if (total <= 0) {
            return items;
        }
        List<PaymentMethodBreakdownDto> result = new ArrayList<>();
        for (PaymentMethodBreakdownDto item : items) {
            result.add(PaymentMethodBreakdownDto.builder()
                    .label(item.getLabel())
                    .value(item.getValue())
                    .orders(item.getOrders())
                    .pct(Math.round((item.getValue() / total) * 1000.0) / 10.0)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewDto getOverview(Long sellerId, String period, String channel) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        String filter = channel != null ? channel.trim() : "All Channels";

        BigDecimal totalSales;
        long totalOrders;
        List<AnalyticsChannelDto> channels;
        List<PaymentMethodBreakdownDto> paymentMethods = getPaymentMethods(sellerId, period);

        if (isPaymentChannel(filter)) {
            String paymentKey = filter.toLowerCase();
            totalSales = BigDecimal.ZERO;
            totalOrders = 0;
            channels = new ArrayList<>();
            for (PaymentMethodBreakdownDto pm : paymentMethods) {
                if (pm.getLabel().equalsIgnoreCase(filter) || pm.getLabel().toLowerCase().contains(paymentKey)) {
                    totalSales = BigDecimal.valueOf(pm.getValue());
                    totalOrders = pm.getOrders();
                    channels.add(AnalyticsChannelDto.builder()
                            .name(pm.getLabel())
                            .amount(totalSales)
                            .orders(totalOrders)
                            .build());
                    break;
                }
            }
            if (channels.isEmpty()) {
                channels.add(AnalyticsChannelDto.builder().name(filter).amount(BigDecimal.ZERO).orders(0).build());
            }
            paymentMethods = channels.isEmpty() ? paymentMethods : List.of(
                    PaymentMethodBreakdownDto.builder()
                            .label(filter)
                            .value(totalSales.doubleValue())
                            .orders(totalOrders)
                            .pct(100)
                            .build());
        } else if (isStatusChannel(filter)) {
            List<Object[]> statusRows = orderItemRepository.sumSalesByStatus(sellerId, range[0], range[1]);
            String statusKey = mapChannelToStatus(filter);
            totalSales = BigDecimal.ZERO;
            totalOrders = 0;
            channels = new ArrayList<>();
            for (Object[] row : statusRows) {
                String status = String.valueOf(row[0]);
                if (status.equalsIgnoreCase(statusKey)) {
                    totalSales = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    totalOrders = row[2] != null ? ((Number) row[2]).longValue() : 0;
                    channels.add(AnalyticsChannelDto.builder()
                            .name(capitalize(status))
                            .amount(totalSales)
                            .orders(totalOrders)
                            .build());
                    break;
                }
            }
            if (channels.isEmpty()) {
                channels.add(AnalyticsChannelDto.builder().name(filter).amount(BigDecimal.ZERO).orders(0).build());
            }
        } else {
            totalSales = orderItemRepository.sumSalesBetween(sellerId, range[0], range[1]);
            totalOrders = orderItemRepository.countDistinctOrdersBetween(sellerId, range[0], range[1]);
            channels = List.of(AnalyticsChannelDto.builder()
                    .name("All Channels")
                    .amount(totalSales != null ? totalSales : BigDecimal.ZERO)
                    .orders(totalOrders)
                    .build());
            List<Object[]> statusRows = orderItemRepository.sumSalesByStatus(sellerId, range[0], range[1]);
            if (!statusRows.isEmpty()) {
                channels = new ArrayList<>();
                for (Object[] row : statusRows) {
                    channels.add(AnalyticsChannelDto.builder()
                            .name(capitalize(String.valueOf(row[0])))
                            .amount(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO)
                            .orders(row[2] != null ? ((Number) row[2]).longValue() : 0)
                            .build());
                }
            }
        }

        BigDecimal sales = totalSales != null ? totalSales : BigDecimal.ZERO;
        double aov = totalOrders > 0
                ? sales.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0;

        return AnalyticsOverviewDto.builder()
                .total(sales)
                .orders(totalOrders)
                .aov(aov)
                .returns(countStatus(statusRowsFor(sellerId, range), "return"))
                .cancels(countStatus(statusRowsFor(sellerId, range), "cancel"))
                .replacements(countStatus(statusRowsFor(sellerId, range), "replacement"))
                .channels(channels)
                .paymentMethods(paymentMethods)
                .build();
    }

    private List<Object[]> statusRowsFor(Long sellerId, LocalDateTime[] range) {
        return orderItemRepository.sumSalesByStatus(sellerId, range[0], range[1]);
    }

    private long countStatus(List<Object[]> rows, String keyword) {
        return rows.stream()
                .filter(r -> String.valueOf(r[0]).toLowerCase().contains(keyword))
                .mapToLong(r -> r[2] != null ? ((Number) r[2]).longValue() : 0)
                .sum();
    }

    private boolean isPaymentChannel(String channel) {
        String c = channel.toLowerCase();
        return c.equals("upi") || c.equals("card") || c.equals("cod") || c.contains("net banking");
    }

    private boolean isStatusChannel(String channel) {
        String c = channel.toLowerCase();
        return c.equals("delivered") || c.equals("pending") || c.equals("canceled")
                || c.equals("cancelled") || c.equals("returns") || c.equals("replacements");
    }

    private String mapChannelToStatus(String channel) {
        return switch (channel.toLowerCase()) {
            case "canceled", "cancelled" -> "cancelled";
            case "returns" -> "returned";
            case "replacements" -> "replacement";
            default -> channel.toLowerCase();
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Other";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String formatPaymentLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Other";
        }
        String lower = raw.toLowerCase();
        if (lower.contains("upi")) return "UPI";
        if (lower.contains("card")) return "Card";
        if (lower.contains("cod") || lower.contains("cash")) return "COD";
        if (lower.contains("net")) return "Net Banking";
        return capitalize(raw);
    }

    private List<SalesTrendPointDto> buildSalesTrendPoints(Long sellerId, LocalDateTime[] range, String period) {
        List<Object[]> rows = orderItemRepository.sumSalesGroupedByDay(sellerId, range[0], range[1]);
        java.util.Map<LocalDate, BigDecimal> byDay = new java.util.HashMap<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            BigDecimal amt = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            byDay.put(day, amt);
        }
        return fillTrendPoints(range, period, byDay);
    }

    private List<SalesTrendPointDto> buildCountTrendPoints(
            List<Object[]> rows,
            LocalDateTime[] range,
            String period) {
        java.util.Map<LocalDate, Double> byDay = new java.util.HashMap<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            double val = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            byDay.put(day, val);
        }
        LocalDate start = range[0].toLocalDate();
        LocalDate end = range[1].toLocalDate().minusDays(1);
        List<SalesTrendPointDto> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            points.add(SalesTrendPointDto.builder()
                    .label(formatTrendLabel(d, period))
                    .value(byDay.getOrDefault(d, 0.0))
                    .build());
        }
        return points;
    }

    private List<SalesTrendPointDto> fillTrendPoints(
            LocalDateTime[] range,
            String period,
            java.util.Map<LocalDate, BigDecimal> byDay) {
        LocalDate start = range[0].toLocalDate();
        LocalDate end = range[1].toLocalDate().minusDays(1);
        List<SalesTrendPointDto> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            BigDecimal amt = byDay.getOrDefault(d, BigDecimal.ZERO);
            points.add(SalesTrendPointDto.builder()
                    .label(formatTrendLabel(d, period))
                    .value(amt.doubleValue())
                    .build());
        }
        return points;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.time.LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private String formatTrendLabel(LocalDate date, String period) {
        if ("year".equalsIgnoreCase(period)) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
        }
        if ("month".equalsIgnoreCase(period)) {
            return String.valueOf(date.getDayOfMonth());
        }
        if ("day".equalsIgnoreCase(period)) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("ha", Locale.ENGLISH)).toLowerCase();
        }
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH));
    }

    private static String formatDiscountLabel(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        if (variant.getDiscountPercentage() != null
                && variant.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getDiscountPercentage().stripTrailingZeros().toPlainString() + "% OFF";
        }
        if (variant.getDiscountAmount() != null
                && variant.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            return INR.format(variant.getDiscountAmount()) + " OFF";
        }
        return null;
    }

    private String resolveImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = mediaBaseUrl == null ? "" : mediaBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }
}
