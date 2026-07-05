package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.DeliveryOption;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductReview;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.mapper.ProductMapper;
import com.ecommerce.authdemo.repository.*;
import com.ecommerce.authdemo.service.SellerStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreServiceImpl implements SellerStoreService {

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryOptionRepository deliveryOptionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public SellerProfileDTO getProfile(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId).orElse(null);
        long productCount = productRepository.countBySellerId(sellerId);
        if (seller == null && productCount == 0) {
            throw new RuntimeException("Seller not found");
        }
        List<ProductDTO> sampleProducts = loadSellerSampleProducts(sellerId, 8);
        double sellerRating = averageSellerRating(sellerId);
        long ordersCompleted = orderItemRepository.countBySellerId(sellerId);
        return buildProfile(
                seller != null ? seller : syntheticSeller(sellerId),
                sampleProducts,
                sellerRating,
                ordersCompleted
        );
    }

    @Override
    public SellerStoreResponseDTO getStore(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId).orElse(null);
        long productCount = productRepository.countBySellerId(sellerId);
        if (seller == null && productCount == 0) {
            throw new RuntimeException("Seller not found");
        }

        long reviewCount = reviewRepository.countActiveBySellerId(sellerId);
        long ordersCompleted = orderItemRepository.countBySellerId(sellerId);
        long fulfilledOrders = orderItemRepository.countFulfilledBySellerId(sellerId);

        Double avgRating = reviewRepository.averageRatingBySellerId(sellerId);
        double sellerRating = avgRating != null ? roundOne(avgRating) : 0.0;

        List<ProductDTO> sampleProducts = loadSellerSampleProducts(sellerId, 20);
        List<ProductDTO> topProducts = sampleProducts.stream().limit(8).toList();

        SellerStoreResponseDTO response = new SellerStoreResponseDTO();
        response.setProfile(buildProfile(
                seller != null ? seller : syntheticSeller(sellerId),
                sampleProducts,
                sellerRating,
                ordersCompleted
        ));
        response.setStats(buildStats(sellerRating, ordersCompleted, fulfilledOrders, sampleProducts));
        response.setAboutText(buildAboutText(
                seller != null ? seller : syntheticSeller(sellerId),
                sampleProducts
        ));
        response.setHighlights(defaultHighlights());
        response.setPolicies(buildPolicies(sellerId, sampleProducts));
        response.setTopProducts(topProducts);
        response.setProductCount(productCount);
        response.setReviewCount(reviewCount);
        response.setQaCount(0L);
        return response;
    }

    @Override
    public Page<ProductDTO> getProducts(Long sellerId, int page, int size) {
        long productCount = productRepository.countBySellerId(sellerId);
        if (productCount == 0 && !sellerRepository.existsById(sellerId)) {
            throw new RuntimeException("Seller not found");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageable);
        return productPage.map(productMapper::toDTO);
    }

    @Override
    public Page<SellerReviewDTO> getReviews(Long sellerId, int page, int size) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new RuntimeException("Seller not found");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ProductReview> reviews = reviewRepository.findActiveBySellerId(sellerId, pageable);
        return reviews.map(this::toReviewDto);
    }

    private SellerProfileDTO buildProfile(
            Seller seller,
            List<ProductDTO> products,
            double sellerRating,
            long ordersCompleted
    ) {
        SellerProfileDTO profile = new SellerProfileDTO();
        profile.setId(seller.getId());
        profile.setBusinessName(trim(seller.getBusinessName()));
        profile.setFirstName(trim(seller.getFirstName()));
        profile.setLastName(trim(seller.getLastName()));
        profile.setEmail(trim(seller.getEmail()));
        profile.setMobileNumber(trim(seller.getMobileNumber()));
        profile.setBranchName(trim(seller.getBranchName()));
        profile.setAddress(trim(seller.getAddress()));
        profile.setState(trim(seller.getState()));
        profile.setPincode(trim(seller.getPincode()));
        profile.setDisplayName(resolveDisplayName(seller));
        profile.setCategoryLabel(resolveCategoryLabel(products));
        if (seller.getCreatedAt() != null) {
            profile.setJoinedLabel("Joined " + seller.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)));
        }
        profile.setLocationLabel(resolveLocation(seller));
        profile.setShipsToLabel("Pan India");
        profile.setVerified(!trim(seller.getGstNumber()).isEmpty());
        profile.setPreferredSeller(ordersCompleted >= 100 && sellerRating >= 4.5);
        return profile;
    }

    private SellerStatsDTO buildStats(
            double sellerRating,
            long ordersCompleted,
            long fulfilledOrders,
            List<ProductDTO> products
    ) {
        SellerStatsDTO stats = new SellerStatsDTO();
        stats.setSellerRating(sellerRating > 0 ? sellerRating : null);
        stats.setSellerRatingLabel(formatRatingLabel(sellerRating));
        stats.setOrdersCompleted(ordersCompleted);
        stats.setOrdersCompletedLabel(formatCountLabel(ordersCompleted, "Orders Completed"));

        if (ordersCompleted > 0) {
            int onTime = (int) Math.round((fulfilledOrders * 100.0) / ordersCompleted);
            stats.setOnTimeDeliveryPercent(onTime);
            stats.setOnTimeDeliveryLabel(onTime + "% On-time Delivery");
            int positive = Math.min(100, Math.max(0, onTime));
            stats.setPositiveFeedbackPercent(positive);
            stats.setPositiveFeedbackLabel(positive + "% Positive Feedback");
        }

        Integer avgDispatch = averageDispatchDays(products);
        if (avgDispatch != null) {
            stats.setAvgDispatchDays(avgDispatch);
            stats.setAvgDispatchDaysLabel(avgDispatch + (avgDispatch == 1 ? " Day" : " Days") + " Avg. Dispatch Time");
        }
        return stats;
    }

    private List<SellerPolicyDTO> buildPolicies(Long sellerId, List<ProductDTO> products) {
        List<SellerPolicyDTO> policies = new ArrayList<>();

        String returnPolicy = products.stream()
                .map(ProductDTO::getReturnPolicy)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse("Standard return policy applies for eligible products.");

        SellerPolicyDTO returns = new SellerPolicyDTO();
        returns.setKey("returns");
        returns.setTitle("Returns & Refunds");
        returns.setSubtitle("Return policy");
        returns.setBody(returnPolicy);
        policies.add(returns);

        List<DeliveryOption> deliveryOptions = sellerId <= Integer.MAX_VALUE
                ? deliveryOptionRepository.findWithFilters(sellerId.intValue(), true)
                : List.of();
        String shippingBody = deliveryOptions.isEmpty()
                ? "Shipping timelines vary by product and delivery location."
                : deliveryOptions.stream()
                .map(opt -> opt.getOptionName() + ": " + opt.getMinDays() + "-" + opt.getMaxDays() + " days"
                        + (opt.getDeliveryInfo() != null && !opt.getDeliveryInfo().isBlank()
                        ? " — " + opt.getDeliveryInfo() : ""))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        SellerPolicyDTO shipping = new SellerPolicyDTO();
        shipping.setKey("shipping");
        shipping.setTitle("Shipping Policy");
        shipping.setSubtitle(deliveryOptions.isEmpty() ? "Delivery information" : deliveryOptions.size() + " option(s)");
        shipping.setBody(shippingBody);
        policies.add(shipping);

        SellerPolicyDTO cancellation = new SellerPolicyDTO();
        cancellation.setKey("cancellation");
        cancellation.setTitle("Cancellation Policy");
        cancellation.setSubtitle("Before dispatch");
        cancellation.setBody("Orders can be cancelled before they are shipped. Contact seller support for help.");
        policies.add(cancellation);

        SellerPolicyDTO privacy = new SellerPolicyDTO();
        privacy.setKey("privacy");
        privacy.setTitle("Privacy Policy");
        privacy.setSubtitle("Data handling");
        privacy.setBody("Customer data is used only to fulfil orders and provide support.");
        policies.add(privacy);

        return policies;
    }

    private List<SellerHighlightDTO> defaultHighlights() {
        return List.of(
                highlight("shield-checkmark", "100% Authentic", "Genuine products"),
                highlight("cube", "Secure Packaging", "Safe delivery"),
                highlight("headset", "Dedicated Support", "Seller assistance"),
                highlight("ribbon", "Premium Quality", "Curated catalogue")
        );
    }

    private SellerHighlightDTO highlight(String icon, String title, String subtitle) {
        SellerHighlightDTO h = new SellerHighlightDTO();
        h.setIcon(icon);
        h.setTitle(title);
        h.setSubtitle(subtitle);
        return h;
    }

    private String buildAboutText(Seller seller, List<ProductDTO> products) {
        String business = resolveDisplayName(seller);
        String category = resolveCategoryLabel(products);
        if (category != null && !category.isBlank()) {
            return business + " offers quality " + category.toLowerCase(Locale.ENGLISH)
                    + " with reliable fulfilment and customer-first service.";
        }
        return business + " is a trusted seller on our marketplace with quality products and reliable service.";
    }

    private SellerReviewDTO toReviewDto(ProductReview review) {
        SellerReviewDTO dto = new SellerReviewDTO();
        dto.setId(review.getId());
        Product product = review.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
        }
        dto.setReviewerName(review.getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setImagePath(review.getImagePath());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    private String resolveDisplayName(Seller seller) {
        String business = trim(seller.getBusinessName());
        if (!business.isEmpty()) return business;
        String full = (trim(seller.getFirstName()) + " " + trim(seller.getLastName())).trim();
        if (!full.isEmpty()) return full;
        return "Seller #" + seller.getId();
    }

    private String resolveLocation(Seller seller) {
        String branch = trim(seller.getBranchName());
        if (!branch.isEmpty()) return branch;
        String state = trim(seller.getState());
        String pin = trim(seller.getPincode());
        if (!state.isEmpty() && !pin.isEmpty()) return state + ", " + pin;
        if (!state.isEmpty()) return state;
        String address = trim(seller.getAddress());
        if (!address.isEmpty()) {
            return address.length() > 60 ? address.substring(0, 57) + "..." : address;
        }
        return "India";
    }

    private String resolveCategoryLabel(List<ProductDTO> products) {
        Long categoryId = products.stream()
                .map(ProductDTO::getCategoryId)
                .filter(id -> id != null && id > 0)
                .findFirst()
                .orElse(null);
        if (categoryId == null) return "General";
        return categoryRepository.findById(categoryId)
                .map(c -> c.getCategoryName())
                .orElse("General");
    }

    private Integer averageDispatchDays(List<ProductDTO> products) {
        List<Integer> mids = products.stream()
                .map(p -> {
                    Integer min = p.getDeliveryTimeMin();
                    Integer max = p.getDeliveryTimeMax();
                    if (min == null && max == null) return null;
                    if (min == null) return max;
                    if (max == null) return min;
                    return (min + max) / 2;
                })
                .filter(v -> v != null && v > 0)
                .toList();
        if (mids.isEmpty()) return null;
        int sum = mids.stream().mapToInt(Integer::intValue).sum();
        return Math.max(1, Math.round((float) sum / mids.size()));
    }

    private String formatRatingLabel(double rating) {
        if (rating <= 0) return "—";
        return String.format(Locale.ENGLISH, "%.1f ★ Seller Rating", rating);
    }

    private String formatCountLabel(long count, String suffix) {
        if (count >= 1000) {
            return String.format(Locale.ENGLISH, "%.1fK+ %s", count / 1000.0, suffix);
        }
        return count + "+ " + suffix;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double averageSellerRating(Long sellerId) {
        Double avgRating = reviewRepository.averageRatingBySellerId(sellerId);
        return avgRating != null ? roundOne(avgRating) : 0.0;
    }

    private List<ProductDTO> loadSellerSampleProducts(Long sellerId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return productRepository.findTop20BySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .limit(safeLimit)
                .map(productMapper::toDTO)
                .toList();
    }

    private Seller syntheticSeller(Long sellerId) {
        Seller seller = new Seller();
        seller.setId(sellerId);
        return seller;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
