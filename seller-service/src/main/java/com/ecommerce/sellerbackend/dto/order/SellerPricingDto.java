package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SellerPricingDto {
    private String subtotal;
    private String shipping;
    private String tax;
    private String discount;
    private String referralDiscount;
    private String walletDeduction;
    private String total;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
}
