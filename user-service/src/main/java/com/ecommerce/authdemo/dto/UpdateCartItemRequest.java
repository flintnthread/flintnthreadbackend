package com.ecommerce.authdemo.dto;

    import lombok.Data;

    @Data
    public class UpdateCartItemRequest {

        private Long cartItemId;

        private Integer quantity;
    }

