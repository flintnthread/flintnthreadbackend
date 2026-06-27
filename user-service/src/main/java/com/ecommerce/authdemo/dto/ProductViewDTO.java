package com.ecommerce.authdemo.dto;


import lombok.Data;

    @Data
    public class ProductViewDTO {

        private Long productId;
        private Long userId;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
    }

