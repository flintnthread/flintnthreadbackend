package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.AddToCartDTO;
import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.CartResponseDTO;
import com.ecommerce.authdemo.service.CartService;
import com.ecommerce.authdemo.exception.CartException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(
            @Valid @RequestBody AddToCartDTO dto) {
        
        log.info("Add to cart request: productId={}, quantity={}", dto.getProductId(), dto.getQuantity());
        
        try {
            CartResponseDTO response = cartService.addToCart(dto);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Item added to cart successfully", response)
            );
        } catch (CartException e) {
            log.error("Cart error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (RuntimeException e) {
            // `SecurityUtil` throws RuntimeException("User not authenticated") when JWT/session is missing.
            String msg = e.getMessage() != null ? e.getMessage() : "Failed to add item to cart";
            if (msg.toLowerCase().contains("not authenticated")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(false, msg, null));
            }
            log.error("Runtime error adding item to cart: {}", msg, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, msg, null));
        } catch (Exception e) {
            log.error("Unexpected error adding to cart: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to add item to cart", null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart() {
        try {
            CartResponseDTO response = cartService.getCart();
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Cart fetched successfully", response)
            );
        } catch (Exception e) {
            log.error("Error fetching cart: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch cart", null));
        }
    }

   @PutMapping("/item/{id}")
public ResponseEntity<ApiResponse<CartResponseDTO>> updateQuantity(
        @PathVariable Long id,
        @RequestParam Integer quantity) {

    log.info("Update quantity request: itemId={}, quantity={}", id, quantity);

    if (quantity == null || quantity == 0) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Quantity cannot be zero", null));
    }

    try {
        // Get existing cart item
        var cartItem = cartService.getCartItemById(id);

        int newQty = cartItem.getQuantity() + quantity;

        // ✅ STOCK VALIDATION - Check before increasing quantity
        if (quantity > 0) {
            Integer availableStock = cartService.getStockByVariantId(cartItem.getVariantId());
            if (availableStock != null && availableStock >= 0 && newQty > availableStock) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(
                                false,
                                "Only " + availableStock + " items available in stock",
                                null
                        ));
            }
        }

        // ✅ REMOVE if qty becomes 0
        if (newQty <= 0) {
            CartResponseDTO response = cartService.removeItem(id);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Item removed successfully", response)
            );
        }

        // ✅ UPDATE quantity
        CartResponseDTO response = cartService.updateQuantity(id, quantity);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Quantity updated successfully", response)
        );

    } catch (ResourceNotFoundException e) {
        log.error("Cart item not found: {}", e.getMessage());
        return ResponseEntity.notFound().build();

    } catch (CartException e) {
        log.error("Cart error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage(), null));

    } catch (Exception e) {
        log.error("Unexpected error updating quantity: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Failed to update quantity", null));
    }
}
    @DeleteMapping("/item/{id}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeItem(
            @PathVariable Long id) {
        log.info("Remove item request: itemId={}", id);

        try {
            CartResponseDTO response = cartService.removeItem(id);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Item removed successfully", response)
            );
        } catch (ResourceNotFoundException e) {
            log.error("Cart item not found: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (CartException e) {
            log.error("Cart error removing item: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error removing item: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to remove item", null));
        }
    }

    // Fallback for clients/environments where DELETE method is blocked.
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeItemPost(
            @RequestBody Map<String, Object> payload) {
        Object rawItemId = payload != null ? payload.get("itemId") : null;
        if (rawItemId == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "itemId is required", null));
        }

        Long id;
        try {
            id = Long.valueOf(String.valueOf(rawItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid itemId", null));
        }

        return removeItem(id);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponseDTO>> clearCart() {
        log.info("Clear cart request");
        
        try {
            cartService.clearCart();
            CartResponseDTO response = cartService.getCart();
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Cart cleared successfully", response)
            );
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to clear cart", null));
        }
    }

    // Fallback for clients/environments where DELETE method is blocked.
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponseDTO>> clearCartPost() {
        return clearCart();
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartCount() {
        try {
            Integer count = cartService.getCartCount();
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Cart count fetched successfully", count)
            );
        } catch (Exception e) {
            log.error("Error fetching cart count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch cart count", 0));
        }
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<ApiResponse<CartResponseDTO>> applyCoupon(
            @RequestParam String code) {
        
        log.info("Apply coupon request: code={}", code);
        
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Coupon code is required", null));
        }
        
        try {
            CartResponseDTO response = cartService.applyCoupon(code.trim());
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Coupon applied successfully", response)
            );
        } catch (CartException e) {
            log.error("Coupon error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error applying coupon: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to apply coupon", null));
        }
    }
}