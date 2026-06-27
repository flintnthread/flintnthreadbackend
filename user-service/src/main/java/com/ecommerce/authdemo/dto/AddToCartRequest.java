package com.ecommerce.authdemo.dto;

    import lombok.Data;

    @Data
    public class AddToCartRequest {

        private Long productId;

        private Long sellerId;

        private Integer quantity;
    }

