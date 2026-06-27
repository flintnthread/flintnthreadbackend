package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.WishlistProductResponse;
import com.ecommerce.authdemo.dto.WishlistRequest;
import com.ecommerce.authdemo.dto.WishlistResponse;
import com.ecommerce.authdemo.service.WishlistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin("*")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService){
        this.wishlistService = wishlistService;
    }

    // -------------------------
    // ADD TO WISHLIST (FULL PRODUCT RESPONSE)
    // -------------------------
    @PostMapping("/add")
    public WishlistResponse addToWishlist(@RequestBody WishlistRequest request){
        return wishlistService.addToWishlist(
                null, // userId not needed, using authenticated user
                request.getProductId(),
                request.getVariantId()
        );
    }

    // -------------------------
    // GET USER WISHLIST
    // -------------------------
    @GetMapping("/user")
    public List<WishlistResponse> getWishlist(){
        return wishlistService.getUserWishlist(null); // userId not needed, using authenticated user
    }

    // -------------------------
    // REMOVE FROM WISHLIST
    // -------------------------
    @DeleteMapping("/remove")
    public String removeFromWishlist(@RequestParam Long productId,
                                     @RequestParam Long variantId){

        wishlistService.removeFromWishlist(null, productId, variantId); // userId not needed, using authenticated user
        return "Product removed from wishlist";
    }

    // -------------------------
    // CHECK PRODUCT IN WISHLIST
    // -------------------------
    @GetMapping("/check")
    public boolean checkWishlist(@RequestParam Long productId){

        return wishlistService.isProductInWishlist(null, productId); // userId not needed, using authenticated user
    }

    // -------------------------
    // CHECK PRODUCT VARIANT IN WISHLIST
    // -------------------------
    @GetMapping("/check-variant")
    public boolean checkVariantWishlist(@RequestParam Long productId,
                                        @RequestParam Long variantId){

        return wishlistService.isProductVariantInWishlist(null, productId, variantId); // userId not needed, using authenticated user
    }
}