package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsChannelDto {
    private String name;
    private BigDecimal amount;
    private long orders;
}
