package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.DashboardChartsResponse;
import com.ecommerce.sellerbackend.dto.DashboardOrderSummaryDto;
import com.ecommerce.sellerbackend.dto.DashboardOverviewDto;
import com.ecommerce.sellerbackend.dto.DashboardPeriodStatsDto;
import com.ecommerce.sellerbackend.dto.DashboardReferralDto;
import com.ecommerce.sellerbackend.dto.DashboardResponse;
import com.ecommerce.sellerbackend.dto.DashboardStatsByPeriodResponse;
import com.ecommerce.sellerbackend.dto.DashboardTopProductDto;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.ReferralCodeService;
import com.ecommerce.sellerbackend.dto.SalesTrendPointDto;
import com.ecommerce.sellerbackend.service.AnalyticsService;
import com.ecommerce.sellerbackend.util.AnalyticsPeriodUtil;
import com.ecommerce.sellerbackend.dto.order.SellerOrderStatsDto;
import com.ecommerce.sellerbackend.entity.Category;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductImage;
import com.ecommerce.sellerbackend.entity.ProductVariant;
import com.ecommerce.sellerbackend.repository.CategoryRepository;
import com.ecommerce.sellerbackend.repository.ProductImageRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductReviewRepository;
import com.ecommerce.sellerbackend.repository.ProductVariantRepository;
import com.ecommerce.sellerbackend.repository.ProductViewRepository;
import com.ecommerce.sellerbackend.repository.OrderItemRepository;
import com.ecommerce.sellerbackend.service.DashboardService;
import com.ecommerce.sellerbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductViewRepository productViewRepository;
    private final OrderItemRepository orderItemRepository;
    private final AnalyticsService analyticsService;
    private final SellerRepository sellerRepository;
    private final ReferralCodeService referralCodeService;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Value("${app.referral.goal:6}")
    private int referralGoal;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long sellerId) {
        SellerOrderStatsDto stats = orderService.statsForSeller(sellerId);
        Double avgRating = productReviewRepository.averageRatingForSeller(sellerId);
        long reviewCount = productReviewRepository.countActiveForSeller(sellerId);
        long views = productViewRepository.countViewsForSeller(sellerId);
        int totalProducts = (int) productRepository.countBySellerId(sellerId);

        DashboardOverviewDto overview = DashboardOverviewDto.builder()
                .orders(stats.getTotalLineItems())
                .sales(stats.getTotalSale())
                .views(views)
                .rating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .salesFormatted(formatInr(stats.getTotalSale()))
                .build();

        DashboardOrderSummaryDto orderSummary = DashboardOrderSummaryDto.builder()
                .pending(stats.getPending())
                .processing(stats.getProcessing())
                .shipped(stats.getShipped())
                .delivered(stats.getDelivered())
                .returns(stats.getReturns())
                .build();

        List<DashboardTopProductDto> topProducts = buildTopProducts(sellerId);
        DashboardReferralDto referral = buildReferral(sellerId);

        return DashboardResponse.builder()
                .overview(overview)
                .orderSummary(orderSummary)
                .topProducts(topProducts)
                .totalProducts(totalProducts)
                .referral(referral)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardChartsResponse getCharts(Long sellerId, String period) {
        String mapped = AnalyticsPeriodUtil.mapDashboardPeriod(period);
        List<SalesTrendPointDto> salesPoints = analyticsService.getSalesTrend(sellerId, mapped);
        List<SalesTrendPointDto> ordersPoints = analyticsService.getOrdersTrend(sellerId, mapped);
        List<SalesTrendPointDto> productsPoints = analyticsService.getProductsTrend(sellerId, mapped);

        BigDecimal totalSales = salesPoints.stream()
                .map(p -> BigDecimal.valueOf(p.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = Math.round(ordersPoints.stream().mapToDouble(SalesTrendPointDto::getValue).sum());
        long totalUnits = Math.round(productsPoints.stream().mapToDouble(SalesTrendPointDto::getValue).sum());

        return DashboardChartsResponse.builder()
                .period(mapped)
                .salesPoints(salesPoints)
                .ordersPoints(ordersPoints)
                .productsPoints(productsPoints)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalUnitsSold(totalUnits)
                .totalSalesFormatted(formatInr(totalSales))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsByPeriodResponse getStatsByPeriod(Long sellerId) {
        Double avgRating = productReviewRepository.averageRatingForSeller(sellerId);
        double rating = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        return DashboardStatsByPeriodResponse.builder()
                .day(buildPeriodStats(sellerId, "day", rating))
                .week(buildPeriodStats(sellerId, "week", rating))
                .month(buildPeriodStats(sellerId, "month", rating))
                .year(buildPeriodStats(sellerId, "year", rating))
                .build();
    }

    private DashboardPeriodStatsDto buildPeriodStats(Long sellerId, String period, double rating) {
        LocalDateTime[] range = AnalyticsPeriodUtil.resolveRange(period);
        BigDecimal sales = orderItemRepository.sumSalesBetween(sellerId, range[0], range[1]);
        if (sales == null) {
            sales = BigDecimal.ZERO;
        }
        long orders = orderItemRepository.countDistinctOrdersBetween(sellerId, range[0], range[1]);
        long views = productViewRepository.countViewsForSellerBetween(sellerId, range[0], range[1]);
        long returns = countReturnsInRange(sellerId, range);

        return DashboardPeriodStatsDto.builder()
                .period(period)
                .orders(orders)
                .sales(sales)
                .salesFormatted(formatInr(sales))
                .views(views)
                .rating(rating)
                .returns(returns)
                .build();
    }

    private long countReturnsInRange(Long sellerId, LocalDateTime[] range) {
        return orderItemRepository.sumSalesByStatus(sellerId, range[0], range[1]).stream()
                .filter(row -> String.valueOf(row[0]).toLowerCase().contains("return"))
                .mapToLong(row -> row[2] != null ? ((Number) row[2]).longValue() : 0)
                .sum();
    }

    private DashboardReferralDto buildReferral(Long sellerId) {
        String referralCode = referralCodeService.ensureReferralCode(sellerId);
        long totalReferred = sellerRepository.countByReferredBySellerId(sellerId);
        long qualifiedReferred = sellerRepository.countQualifiedReferrals(sellerId);
        return DashboardReferralDto.builder()
                .referralCode(referralCode)
                .totalReferred(totalReferred)
                .qualifiedReferred(qualifiedReferred)
                .goal(referralGoal)
                .build();
    }

    private List<DashboardTopProductDto> buildTopProducts(Long sellerId) {
        List<Object[]> rows = orderItemRepository.topProductIdsByQuantity(sellerId);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .limit(8)
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

        List<DashboardTopProductDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            long sold = ((Number) row[1]).longValue();
            Product product = products.get(productId);
            if (product == null) {
                continue;
            }
            BigDecimal price = variants.getOrDefault(productId, List.of()).stream()
                    .map(ProductVariant::getFinalPrice)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            String image = images.getOrDefault(productId, List.of()).stream()
                    .findFirst()
                    .map(img -> resolveImageUrl(img.getImagePath()))
                    .orElse("");

            result.add(DashboardTopProductDto.builder()
                    .id(String.valueOf(productId))
                    .name(cleanProductName(product.getName()))
                    .price(formatInr(price))
                    .sold(sold)
                    .image(image)
                    .category(categoryNames.getOrDefault(product.getCategoryId(), "—"))
                    .build());

            if (result.size() >= 8) {
                break;
            }
        }
        return result;
    }

    private String formatInr(BigDecimal amount) {
        if (amount == null) {
            return INR.format(0);
        }
        return INR.format(amount);
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
