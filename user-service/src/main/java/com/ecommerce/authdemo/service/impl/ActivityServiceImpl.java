package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ActivitySummaryDTO;
import com.ecommerce.authdemo.dto.RecentlyViewedActivityDTO;
import com.ecommerce.authdemo.dto.UserActivityReviewDTO;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductImage;
import com.ecommerce.authdemo.entity.ProductReview;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.repository.CategoryRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductViewRepository;
import com.ecommerce.authdemo.repository.ReviewRepository;
import com.ecommerce.authdemo.mapper.ProductMapper;
import com.ecommerce.authdemo.repository.WishlistRepository;
import com.ecommerce.authdemo.service.ActivityService;
import com.ecommerce.authdemo.service.CustomerPriceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final CustomerPriceResolver customerPriceResolver;

    @Override
    @Transactional(readOnly = true)
    public ActivitySummaryDTO getSummary(Long userId, String sessionId, Long authenticatedUserId) {
        List<RecentlyViewedActivityDTO> recent = getRecentlyViewed(userId, sessionId, null, null, null, null);
        int wishlistCount = 0;
        Long reviewUserId = authenticatedUserId != null ? authenticatedUserId : userId;
        int reviewsCount = 0;
        if (reviewUserId != null && reviewUserId > 0) {
            reviewsCount = (int) reviewRepository.countByUserIdAndStatusTrue(reviewUserId);
        }
        if (authenticatedUserId != null && authenticatedUserId > 0) {
            wishlistCount = (int) wishlistRepository.countByUserId(authenticatedUserId);
        }
        return ActivitySummaryDTO.builder()
                .recentlyViewedCount(recent.size())
                .wishlistCount(wishlistCount)
                .reviewsCount(reviewsCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewedActivityDTO> getRecentlyViewed(
            Long userId,
            String sessionId,
            String search,
            Long categoryId,
            String sort,
            String availability
    ) {
        if ((userId == null || userId <= 0) && (sessionId == null || sessionId.isBlank())) {
            return List.of();
        }

        List<Object[]> rows = productViewRepository.findRecentlyViewedSummaries(userId, sessionId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<Long, LocalDateTime> viewedAtByProduct = new LinkedHashMap<>();
        List<Long> productIds = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long pid = ((Number) row[0]).longValue();
            LocalDateTime viewedAt = toLocalDateTime(row[1]);
            productIds.add(pid);
            viewedAtByProduct.put(pid, viewedAt);
        }

        if (productIds.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findAllWithImagesAndVariantsByIdIn(productIds).stream()
                .filter(ProductCatalogVisibility::isVisibleToUsers)
                .toList();
        Map<Long, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Map<Long, String> categoryNames = loadCategoryNames(
                products.stream()
                        .map(Product::getCategoryId)
                        .filter(Objects::nonNull)
                        .map(Integer::longValue)
                        .collect(Collectors.toSet())
        );

        List<RecentlyViewedActivityDTO> items = new ArrayList<>();
        for (Long pid : productIds) {
            Product product = productById.get(pid);
            if (product == null) {
                continue;
            }
            RecentlyViewedActivityDTO dto = mapProductToActivity(product, viewedAtByProduct.get(pid), categoryNames);
            if (dto != null) {
                items.add(dto);
            }
        }

        String q = search != null ? search.trim().toLowerCase() : "";
        if (!q.isEmpty()) {
            items = items.stream()
                    .filter(i -> i.getName() != null && i.getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
        if (categoryId != null && categoryId > 0) {
            long cid = categoryId;
            items = items.stream()
                    .filter(i -> i.getCategoryId() != null && i.getCategoryId() == cid)
                    .collect(Collectors.toList());
        }
        if (availability != null && !availability.isBlank()) {
            String av = availability.trim().toLowerCase();
            if ("in_stock".equals(av)) {
                items = items.stream().filter(i -> Boolean.TRUE.equals(i.getInStock())).collect(Collectors.toList());
            } else if ("out_of_stock".equals(av)) {
                items = items.stream().filter(i -> !Boolean.TRUE.equals(i.getInStock())).collect(Collectors.toList());
            }
        }
        if (sort != null && !sort.isBlank()) {
            String s = sort.trim().toLowerCase();
            if ("price_asc".equals(s)) {
                items.sort(Comparator.comparing(
                        i -> i.getSellingPrice() != null ? i.getSellingPrice() : BigDecimal.ZERO
                ));
            } else if ("price_desc".equals(s)) {
                items.sort(Comparator.comparing(
                        (RecentlyViewedActivityDTO i) ->
                                i.getSellingPrice() != null ? i.getSellingPrice() : BigDecimal.ZERO
                ).reversed());
            }
        }

        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityReviewDTO> getMyReviews(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return reviewRepository.findByUserIdAndStatusTrueOrderByCreatedAtDesc(userId).stream()
                .map(this::mapReview)
                .collect(Collectors.toList());
    }

    private UserActivityReviewDTO mapReview(ProductReview review) {
        Product product = review.getProduct();
        String productName = product != null ? product.getName() : "Product";
        String imageUrl = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().stream()
                    .sorted(Comparator.comparing(
                            (ProductImage img) -> Boolean.FALSE.equals(img.getIsPrimary()),
                            Comparator.naturalOrder()
                    ))
                    .map(ProductImage::getImagePath)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(productMapper::resolveImageUrl)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return UserActivityReviewDTO.builder()
                .id(review.getId())
                .productId(product != null ? product.getId() : null)
                .productName(productName)
                .productImageUrl(imageUrl)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private RecentlyViewedActivityDTO mapProductToActivity(
            Product product,
            LocalDateTime viewedAt,
            Map<Long, String> categoryNames
    ) {
        ProductVariant variant = pickVariant(product);
        BigDecimal selling = variant != null
                ? customerPriceResolver.resolveCustomerUnitPrice(product, variant)
                : null;
        BigDecimal mrp = variant != null
                ? customerPriceResolver.resolveCustomerStrikeMrp(product, variant)
                : null;
        Integer stock = variant != null ? variant.getStock() : null;
        boolean inStock = stock != null ? stock > 0 : true;

        Long categoryId = product.getCategoryId() != null ? product.getCategoryId().longValue() : null;
        String categoryName = categoryId != null
                ? categoryNames.getOrDefault(categoryId, "Category")
                : "Category";

        return RecentlyViewedActivityDTO.builder()
                .productId(product.getId())
                .variantId(variant != null ? variant.getId() : null)
                .name(product.getName())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .imageUrl(resolveImage(product, variant))
                .sellingPrice(selling)
                .mrpPrice(mrp)
                .inStock(inStock)
                .stockQty(stock)
                .viewedAt(viewedAt)
                .build();
    }

    private ProductVariant pickVariant(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }
        return product.getVariants().stream()
                .filter(v -> v.getStock() != null && v.getStock() > 0)
                .findFirst()
                .orElse(product.getVariants().get(0));
    }

    private String resolveImage(Product product, ProductVariant variant) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        Long variantId = variant != null ? variant.getId() : null;
        java.util.Collection<ProductImage> images = product.getImages();
        Optional<ProductImage> match = images.stream()
                .filter(img -> variantId == null || Objects.equals(img.getVariantId(), variantId))
                .sorted(Comparator.comparing(
                        (ProductImage img) -> Boolean.FALSE.equals(img.getIsPrimary()),
                        Comparator.naturalOrder()
                ))
                .findFirst();
        if (match.isEmpty()) {
            match = product.getImages().stream().findFirst();
        }
        String path = match.map(ProductImage::getImagePath).orElse(null);
        return path != null ? productMapper.resolveImageUrl(path) : null;
    }

    private Map<Long, String> loadCategoryNames(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> out = new HashMap<>();
        for (Category c : categoryRepository.findAllById(categoryIds)) {
            if (c.getCategoryName() != null) {
                out.put(c.getId(), c.getCategoryName());
            }
        }
        return out;
    }

    private LocalDateTime toLocalDateTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (raw instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (raw instanceof java.util.Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime();
        }
        return null;
    }
}
