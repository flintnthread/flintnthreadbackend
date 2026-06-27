package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

    @Data
    @Builder
    public class RetryPaymentResponseDTO {

        private String razorpayOrderId;

        private Double amount;

        private String currency;

        private String key;

        private String orderNumber;
    }

