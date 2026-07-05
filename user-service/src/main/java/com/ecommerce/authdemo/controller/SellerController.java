package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.SellerProfileDTO;
import com.ecommerce.authdemo.dto.SellerReviewDTO;
import com.ecommerce.authdemo.dto.SellerStoreResponseDTO;
import com.ecommerce.authdemo.service.SellerStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerStoreService sellerStoreService;

    @GetMapping("/{sellerId}/profile")
    public SellerProfileDTO getProfile(@PathVariable Long sellerId) {
        return sellerStoreService.getProfile(sellerId);
    }

    @GetMapping("/{sellerId}/store")
    public SellerStoreResponseDTO getStore(@PathVariable Long sellerId) {
        return sellerStoreService.getStore(sellerId);
    }

    @GetMapping("/{sellerId}/products")
    public Page<ProductDTO> getProducts(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerStoreService.getProducts(sellerId, page, size);
    }

    @GetMapping("/{sellerId}/reviews")
    public Page<SellerReviewDTO> getReviews(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerStoreService.getReviews(sellerId, page, size);
    }
}
