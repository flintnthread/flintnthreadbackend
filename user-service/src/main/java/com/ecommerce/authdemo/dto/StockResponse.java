package com.ecommerce.authdemo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockResponse {
        private Long variantId;
        private Integer stock;
        private String message;

        public StockResponse(Long variantId, Integer stock, String message) {
            this.variantId = variantId;
            this.stock = stock;
            this.message = message;
        }

        // getters & setters
    }

