package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

    @Data
    @Builder
    public class OrderTrackingDTO {

        private String status;

        private String description;

        private String location;

        private LocalDateTime timestamp;
    }

