package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
    public class CartItemDTO {
        private Long productId;
        private Long variantId;
        private Integer quantity;
        private Double price;
        private String image;
        private Double total;

}

