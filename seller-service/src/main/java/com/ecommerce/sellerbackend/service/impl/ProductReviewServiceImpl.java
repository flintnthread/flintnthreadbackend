package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.ProductReviewResponse;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductReview;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.ProductReviewRepository;
import com.ecommerce.sellerbackend.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;

    @Value("${app.media.public-base-url:}")
    private String mediaBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> listForSeller(Long sellerId) {
        List<ProductReview> reviews = productReviewRepository.findActiveForSeller(sellerId);
        return mapReviews(reviews, sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> listForProduct(Long sellerId, Long productId) {
        productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        List<ProductReview> reviews = productReviewRepository
                .findByProductIdAndStatusOrderByCreatedAtDesc(productId, 1);
        return mapReviews(reviews, sellerId);
    }

    private List<ProductReviewResponse> mapReviews(List<ProductReview> reviews, Long sellerId) {
        Map<Long, String> productNames = productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        return reviews.stream().map(r -> {
            String comment = r.getComment() != null ? r.getComment().trim() : "";
            String title = comment.length() > 60 ? comment.substring(0, 57) + "..." : comment;
            if (title.isBlank()) {
                title = "Review";
            }
            return ProductReviewResponse.builder()
                    .id(r.getId())
                    .productId(r.getProductId())
                    .productName(productNames.getOrDefault(r.getProductId(), "Product"))
                    .customerName(r.getName())
                    .customerAvatar("")
                    .rating(r.getRating() != null ? r.getRating() : 0)
                    .title(title)
                    .description(comment)
                    .date(r.getCreatedAt() != null ? r.getCreatedAt().format(DISPLAY_DATE) : "")
                    .verified(r.getUserId() != null)
                    .imageUrl(resolveImageUrl(r.getImagePath()))
                    .sellerReply(r.getSellerReply() != null ? r.getSellerReply() : "")
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public ProductReviewResponse reply(Long sellerId, Long reviewId, String replyText) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        productRepository.findByIdAndSellerId(review.getProductId(), sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        if (replyText == null || replyText.isBlank()) {
            throw new IllegalArgumentException("Reply text is required.");
        }
        review.setSellerReply(replyText.trim());
        productReviewRepository.save(review);
        return mapReviews(List.of(review), sellerId).get(0);
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
