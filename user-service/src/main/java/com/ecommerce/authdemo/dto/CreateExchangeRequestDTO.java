package com.ecommerce.authdemo.dto;

import lombok.Data;

    @Data
    public class CreateExchangeRequestDTO {

        private Long orderId;

        private Long orderItemId;

        private Long exchangeColor;

        private Long exchangeSize;

        private String reason;

        private String description;
    }

