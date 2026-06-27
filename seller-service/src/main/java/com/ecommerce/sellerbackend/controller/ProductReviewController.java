package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.ProductReviewResponse;
import com.ecommerce.sellerbackend.dto.ReviewReplyRequest;
import com.ecommerce.sellerbackend.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final ProductReviewService productReviewService;

    @GetMapping
    public List<ProductReviewResponse> list(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam(required = false) Long productId) {
        Long id = requireSellerId(sellerId);
        if (productId != null) {
            return productReviewService.listForProduct(id, productId);
        }
        return productReviewService.listForSeller(id);
    }

    @GetMapping("/product/{productId}")
    public List<ProductReviewResponse> listForProduct(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long productId) {
        return productReviewService.listForProduct(requireSellerId(sellerId), productId);
    }

    @PostMapping("/{reviewId}/reply")
    public ProductReviewResponse reply(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReplyRequest request) {
        return productReviewService.reply(requireSellerId(sellerId), reviewId, request.getReply());
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
