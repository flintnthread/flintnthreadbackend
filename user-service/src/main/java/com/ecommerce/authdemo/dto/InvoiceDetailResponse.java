package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailResponse {
    private Integer id;
    private Integer orderId;
    private String orderNumber;
    private OrderDetails orderDetails;
    private String invoiceNumber;
    private String invoicePath;
    private String invoiceDate;
    private String orderDate;
    private SellerDetails seller;
    private PartyDetails billTo;
    private PartyDetails shipTo;
    private List<InvoiceItemDetails> items;
    private Totals totals;
    private PaymentDetails payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerDetails {
        private String name;
        private String address;
        private String phone;
        private String email;
        private String gstin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyDetails {
        private String name;
        private String address;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItemDetails {
        private Long productId;
        private Long variantId;
        private String productName;
        private String color;
        private String size;
        private String description;
        private String hsnCode;
        private Integer quantity;
        private Double unitPrice;
        private Double taxPercent;
        private Double taxAmount;
        private Double lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private Double subtotalBeforeTax;
        private Double shippingAmount;
        private Double grandTotal;
        private Double cgstAmount;
        private Double sgstAmount;
        private Double igstAmount;
        private Double totalGst;
        private Double igstRatePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetails {
        private String paymentMethod;
        private String paymentStatus;
        private String transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetails {
        private Long id;
        private Long userId;
        private String orderNumber;
        private Double totalAmount;
        private Double shippingAmount;
        private Double taxAmount;
        private Double discountAmount;
        private String paymentMethod;
        private String paymentStatus;
        private String orderStatus;
        private String shippingName;
        private String shippingPhone;
        private String shippingEmail;
        private String shippingAddress1;
        private String shippingCity;
        private String shippingState;
        private String shippingPincode;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String shiprocketAwbCode;
        private String shiprocketCourierName;
        private String shiprocketTrackingUrl;
        private String shiprocketStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
