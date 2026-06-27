package com.ecommerce.authdemo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemResponseDTO {

    private Long itemId;
    private Long productId;

    private String name;
    private String imageUrl;

    private String productName;

    // ✅ Selling price (actual price user pays)
    private BigDecimal price;

    // ✅ Original price (MRP)
    private BigDecimal originalPrice;

    /** Unit selling price (same semantics as {@code price}; explicit for clients). */
    private BigDecimal sellingPrice;

    /** Unit MRP / list price before line discount. */
    private BigDecimal mrpPrice;

    private Integer quantity;

    // ✅ Total price = price * quantity
    private BigDecimal total;

    private Long variantId;

    private String size;
    private String color;

    /** Display color name (same as {@code color}; explicit for clients). */
    private String colorName;

    // 🔥 VERY IMPORTANT FIELD (for real-time stock in frontend)
    private Integer availableStock;

    // Flag to indicate if item is out of stock (quantity > availableStock or stock == 0)
    private Boolean outOfStock;
}