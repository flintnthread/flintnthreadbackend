package com.ecommerce.authdemo.dto;


import lombok.Data;

    @Data
    public class DeliveryCheckDTO {

        private Long productId;
        private Long pincodeId;
        private boolean deliverable;
    }

