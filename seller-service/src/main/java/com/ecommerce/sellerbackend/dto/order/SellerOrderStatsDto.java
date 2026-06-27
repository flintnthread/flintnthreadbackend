package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SellerOrderStatsDto {
    private int totalLineItems;
    private int totalOrders;
    private int allItems;
    private int pending;
    private int processing;
    private int shipped;
    private int delivered;
    private int returns;
    private int cancelled;
    private BigDecimal totalSale;
}
