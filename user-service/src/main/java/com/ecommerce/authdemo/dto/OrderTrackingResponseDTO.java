package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

    @Data
    @Builder
    public class OrderTrackingResponseDTO {

        private Long orderId;

        private String orderNumber;

        private String awbCode;

        private String courierName;

        private String trackingUrl;

        private String currentStatus;

        private List<OrderTrackingDTO> timeline;
    }

