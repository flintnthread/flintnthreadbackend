package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class ProductListDTO {

        private Long id;

        private String name;

        private String sku;

        private Long categoryId;

        private Long subcategoryId;

        private String status;

        private String image;

    }

