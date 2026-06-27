package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.WishlistProductResponse;
import com.ecommerce.authdemo.dto.WishlistResponse;

import java.util.List;

public interface WishlistService {

    WishlistResponse addToWishlist(Long userId, Long productId, Long variantId);
    List<WishlistResponse> getUserWishlist(Long userId);
    void removeFromWishlist(Long userId, Long productId, Long variantId);
    boolean isProductInWishlist(Long userId, Long productId);
    boolean isProductVariantInWishlist(Long userId, Long productId, Long variantId);
}