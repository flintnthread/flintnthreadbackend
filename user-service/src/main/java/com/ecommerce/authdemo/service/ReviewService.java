package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CreateReviewRequestDTO;
import com.ecommerce.authdemo.dto.ReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    boolean canCurrentUserReviewProduct(Long productId);

    ReviewResponseDTO addReview(CreateReviewRequestDTO request);

    ReviewResponseDTO updateReview(Long reviewId, CreateReviewRequestDTO request);

    void softDeleteReview(Long reviewId);

    List<ReviewResponseDTO> getReviewsByProduct(Long productId);

    List<ReviewResponseDTO> getAllReviewsByProduct(Long productId);

    Page<ReviewResponseDTO> getReviewsByProduct(Long productId, Pageable pageable);

    Page<ReviewResponseDTO> getAllReviewsByProduct(Long productId, Pageable pageable);

    List<ReviewResponseDTO> getReviewsByUser(Long userId);
}
