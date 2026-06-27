package com.ecommerce.authdemo.controller;


import com.ecommerce.authdemo.dto.CreateReviewRequestDTO;
import com.ecommerce.authdemo.dto.ReviewResponseDTO;
import com.ecommerce.authdemo.service.ReviewService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> addReview(@Valid @RequestBody CreateReviewRequestDTO review) {
        return ResponseEntity.ok(reviewService.addReview(review));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequestDTO review
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, review));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> softDeleteReview(@PathVariable Long reviewId) {
        reviewService.softDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public list: only active reviews (`status = true`), newest first.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponseDTO>> getMyReviews() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/product/{productId}/page")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsPage(
            @PathVariable Long productId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    /**
     * Admin/support list: includes active + inactive.
     */
    @GetMapping("/product/{productId}/all")
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getAllReviewsByProduct(productId));
    }

    @GetMapping("/product/{productId}/all/page")
    public ResponseEntity<Page<ReviewResponseDTO>> getAllReviewsPage(
            @PathVariable Long productId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getAllReviewsByProduct(productId, pageable));
    }
}

