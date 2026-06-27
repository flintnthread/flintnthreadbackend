package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_amount", precision = 10, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "referral_discount_amount", precision = 10, scale = 2)
    private BigDecimal referralDiscountAmount;

    @Column(name = "wallet_deduction", precision = 10, scale = 2)
    private BigDecimal walletDeduction;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "seller_payment_status")
    private String sellerPaymentStatus;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Column(name = "shipping_name")
    private String shippingName;

    @Column(name = "shipping_email")
    private String shippingEmail;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "shipping_address1")
    private String shippingAddress1;

    @Column(name = "shipping_address2")
    private String shippingAddress2;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "shipping_state")
    private String shippingState;

    @Column(name = "shipping_country")
    private String shippingCountry;

    @Column(name = "shipping_pincode")
    private String shippingPincode;

    @Column(name = "billing_name")
    private String billingName;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(name = "billing_phone")
    private String billingPhone;

    @Column(name = "billing_address1")
    private String billingAddress1;

    @Column(name = "billing_address2")
    private String billingAddress2;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_state")
    private String billingState;

    @Column(name = "billing_country")
    private String billingCountry;

    @Column(name = "billing_pincode")
    private String billingPincode;

    @Column(name = "order_notes", columnDefinition = "TEXT")
    private String orderNotes;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "gst_info", columnDefinition = "TEXT")
    private String gstInfo;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "shiprocket_order_id", length = 100)
    private String shiprocketOrderId;

    @Column(name = "shiprocket_shipment_id", length = 100)
    private String shiprocketShipmentId;

    @Column(name = "shiprocket_awb_code", length = 100)
    private String shiprocketAwbCode;

    @Column(name = "shiprocket_courier_name", length = 100)
    private String shiprocketCourierName;

    @Column(name = "shiprocket_status", length = 50)
    private String shiprocketStatus;

    @Column(name = "shiprocket_tracking_url", columnDefinition = "TEXT")
    private String shiprocketTrackingUrl;

    @Column(name = "shiprocket_pushed_at")
    private LocalDateTime shiprocketPushedAt;

    @Column(name = "shiprocket_synced_at")
    private LocalDateTime shiprocketSyncedAt;

    @Column(name = "referral_discount_percent", precision = 5, scale = 2)
    private BigDecimal referralDiscountPercent;

    @Column(name = "address_id")
    private Long addressId;
}
