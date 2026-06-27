package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentlyViewedActivityDTO {
    private Long productId;
    private Long variantId;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private BigDecimal sellingPrice;
    private BigDecimal mrpPrice;
    private Boolean inStock;
    private Integer stockQty;
    private LocalDateTime viewedAt;
}
