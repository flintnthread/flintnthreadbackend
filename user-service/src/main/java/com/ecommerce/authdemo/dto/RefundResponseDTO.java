package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

    @Data
    @Builder
    public class RefundResponseDTO {

        private Long id;

        private Long orderId;

        private Long returnId;

        private Double refundAmount;

        private String refundStatus;

        private String refundType;

        private String razorpayRefundId;

    private String remarks;

    private LocalDateTime createdAt;

    private Boolean walletCredited;

    private Double walletCreditAmount;
}

