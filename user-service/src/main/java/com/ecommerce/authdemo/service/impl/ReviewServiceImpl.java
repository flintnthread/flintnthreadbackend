package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CreateReviewRequestDTO;
import com.ecommerce.authdemo.dto.ReviewResponseDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductReview;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ReviewRepository;
import com.ecommerce.authdemo.service.ReviewService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final SecurityUtil securityUtil;

    @Override
    public boolean canCurrentUserReviewProduct(Long productId) {
        if (productId == null || productId <= 0) return false;
        Long userId = securityUtil.getCurrentUserId();
        return orderItemRepository.existsPurchasedProductForUser(userId, productId);
    }

    @Override
    @Transactional
    public ReviewResponseDTO addReview(CreateReviewRequestDTO request) {
        if (!canCurrentUserReviewProduct(request.getProductId())) {
            throw new IllegalStateException("You can review this product only after placing an order.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
        Long userId = securityUtil.getCurrentUserId();

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUserId(userId);
        review.setName(request.getName().trim());
        review.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        review.setImagePath(request.getImagePath() != null ? request.getImagePath().trim() : null);
        review.setStatus(request.getStatus() != null ? request.getStatus() : true);

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponseDTO updateReview(Long reviewId, CreateReviewRequestDTO request) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        Long userId = securityUtil.getCurrentUserId();

        if (review.getUserId() != null && !review.getUserId().equals(userId)) {
            throw new IllegalStateException("You can update only your own review.");
        }

        Long reviewProductId = request.getProductId() != null
                ? request.getProductId()
                : (review.getProduct() != null ? review.getProduct().getId() : null);
        if (!canCurrentUserReviewProduct(reviewProductId)) {
            throw new IllegalStateException("You can review this product only after placing an order.");
        }

        if (request.getProductId() != null
                && review.getProduct() != null
                && !request.getProductId().equals(review.getProduct().getId())) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
            review.setProduct(product);
        }
        review.setUserId(userId);
        review.setName(request.getName().trim());
        review.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        review.setImagePath(request.getImagePath() != null ? request.getImagePath().trim() : null);
        if (request.getStatus() != null) {
            review.setStatus(request.getStatus());
        }
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void softDeleteReview(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        Long userId = securityUtil.getCurrentUserId();
        if (review.getUserId() != null && !review.getUserId().equals(userId)) {
            throw new IllegalStateException("You can delete only your own review.");
        }
        review.setStatus(false);
        reviewRepository.save(review);
    }

    @Override
    public List<ReviewResponseDTO> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProduct_IdAndStatusTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponseDTO> getAllReviewsByProduct(Long productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Page<ReviewResponseDTO> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProduct_IdAndStatusTrueOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<ReviewResponseDTO> getAllReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<ReviewResponseDTO> getReviewsByUser(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return reviewRepository.findByUserIdAndStatusTrueOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReviewResponseDTO toResponse(ProductReview review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .userId(review.getUserId())
                .name(review.getName())
                .email(review.getEmail())
                .rating(review.getRating())
                .comment(review.getComment())
                .imagePath(review.getImagePath())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
