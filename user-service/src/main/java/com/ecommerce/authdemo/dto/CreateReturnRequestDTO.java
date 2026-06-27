package com.ecommerce.authdemo.dto;


import lombok.Data;

    @Data
    public class CreateReturnRequestDTO {

        private Long orderId;

        private Long orderItemId;

        private String reason;

        private String description;

        private String solution;
    }

