package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class ProductImageDTO {

        private Long id;

        private Long productId;

        private String imagePath;

        /** Absolute URL for clients (Expo, web). Populated when app.media.public-base-url is set. */
        private String imageUrl;

        private Boolean isPrimary;

        private Integer sortOrder;

        private Long variantId;
    }

