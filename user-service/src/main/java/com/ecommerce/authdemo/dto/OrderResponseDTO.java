package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderResponseDTO {

    private Long orderId;
    private String orderNumber;

    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;

    private Double totalAmount;
    private Double finalAmount;
    /** FNT Wallet applied at checkout (INR). */
    private Double walletAmountUsed;
    private Double shippingAmount;
    private Double discountAmount;
    /** Grand order value = payable (totalAmount) + wallet used. Shown as Order total on shopper UI. */
    private Double grandTotal;

  private Integer totalItems;
  /** Sum of line quantities (e.g. 2 units of one SKU = 2). */
  private Integer totalQuantity;
  private String firstProductImage;
    private String createdDate;

    private String shippingAddress;

    private String shippingName;
    private String shippingPhone;
    private String shippingEmail;
    private String shippingAddress1;
    private String shippingAddress2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingCountry;

    private Double taxAmount;
    private String razorpayPaymentId;

    private List<OrderItemDTO> items;

    /** Shiprocket / logistics (null until created or on failure). */
    private String shiprocketAwbCode;
    private String shiprocketTrackingUrl;
    private String shiprocketCourierName;
    private String shiprocketStatus;

    private Double totalWeight;
}