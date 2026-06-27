package com.ecommerce.authdemo.dto;

    import lombok.Data;

    @Data
    public class CheckoutRequest {

        private Long addressId;

        private String paymentMethod;

        private String couponCode;
    }

