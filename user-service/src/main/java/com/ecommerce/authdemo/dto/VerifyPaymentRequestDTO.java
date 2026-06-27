package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class VerifyPaymentRequestDTO {

        private Long orderId;

        private String razorpayOrderId;

        private String razorpayPaymentId;

        private String razorpaySignature;
    }

