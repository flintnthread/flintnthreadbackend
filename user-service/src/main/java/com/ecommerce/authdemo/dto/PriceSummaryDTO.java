package com.ecommerce.authdemo.dto;

import lombok.Data;
import java.math.BigDecimal;

    @Data
    public class PriceSummaryDTO {

        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal deliveryCharge;
        private BigDecimal finalTotal;
    }

