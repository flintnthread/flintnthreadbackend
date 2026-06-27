package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WishlistResponse {

    private Long wishlistId;
    private Long productId;
    private Long variantId;
    private String productName;
    private String image; // Primary image (backward compatibility)
    private String imageUrl; // Primary image URL (full URL)
    private List<ProductImageDTO> images; // All images like ProductDTO
    private String size;
    private String color;
    private Boolean inStock;
    private BigDecimal sellingPrice;
    private BigDecimal mrpPrice;
    private LocalDateTime addedAt;
}