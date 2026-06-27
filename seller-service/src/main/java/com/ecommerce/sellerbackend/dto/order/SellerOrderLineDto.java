package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SellerOrderLineDto {
    private Integer lineItemId;
    private Long productId;
    private Long variantId;
    private Long sellerId;
    private String name;
    private String variant;
    private String sku;
    private int qty;
    private String price;
    private BigDecimal priceAmount;
    private BigDecimal subtotalAmount;
    private String image;
    private String hsnCode;
    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal packageDeadWeight;
    private BigDecimal volumetricWeight;
    private BigDecimal chargeableWeight;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal tax;
    private String status;
    private String uiStatus;
    private String color;
    private String size;
    private String sellerName;
    private List<OrderItemCustomDetailDto> customDetails;
}
