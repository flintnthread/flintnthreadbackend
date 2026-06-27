package com.ecommerce.authdemo.dto;


import lombok.Builder;
import lombok.Data;

    @Data
    @Builder
    public class ReturnMediaResponseDTO {

        private Long id;

        private String imageUrl;
    }

