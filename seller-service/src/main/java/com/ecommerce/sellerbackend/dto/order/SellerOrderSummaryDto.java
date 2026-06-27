package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SellerOrderSummaryDto {
    private String id;
    private Long orderId;
    private String date;
    private String product;
    private String variant;
    private int qty;
    private String price;
    private BigDecimal priceAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String status;
    private String customer;
    private String image;
    private String extra;
    private int itemCount;
}
