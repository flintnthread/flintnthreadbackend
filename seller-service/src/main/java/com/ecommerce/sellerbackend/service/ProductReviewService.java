package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.ProductReviewResponse;

import java.util.List;

public interface ProductReviewService {
    List<ProductReviewResponse> listForSeller(Long sellerId);

    List<ProductReviewResponse> listForProduct(Long sellerId, Long productId);

    ProductReviewResponse reply(Long sellerId, Long reviewId, String replyText);
}
