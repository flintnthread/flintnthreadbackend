package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AddToCartDTO;
import com.ecommerce.authdemo.dto.CartResponseDTO;
import com.ecommerce.authdemo.entity.Cart;

public interface CartService {

    CartResponseDTO addToCart(AddToCartDTO dto);

    CartResponseDTO getCart();

    // ✅ change = +1 / -1 (IMPORTANT for your frontend)
    CartResponseDTO updateQuantity(Long itemId, Integer change);

    CartResponseDTO removeItem(Long itemId);

    CartResponseDTO applyCoupon(String code);

    Integer getCartCount();

    void clearCart();

    // ✅ Used for stock validation in controller (if needed)
    Cart getCartItemById(Long itemId);

    // ✅ Get current stock for a variant
    Integer getStockByVariantId(Long variantId);
}