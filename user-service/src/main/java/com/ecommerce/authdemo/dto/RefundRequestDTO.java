package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class RefundRequestDTO {

        private Long orderId;

        private Long returnId;

        private Double refundAmount;

        private String remarks;
    }

