package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.math.BigDecimal;

    @Data
    public class OrderItemResponse {

        private Long productId;

        private Integer quantity;

        private BigDecimal price;
    }

