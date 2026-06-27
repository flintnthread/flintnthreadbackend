package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "total_amount")
    private Double totalAmount;

    /** FNT Wallet debited at checkout for this order (INR). Maps to orders.wallet_deduction. */
    @Column(name = "wallet_deduction")
    private Double walletAmountUsed;

    @Column(name = "shipping_amount")
    private Double shippingAmount;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "shipping_name")
    private String shippingName;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "shipping_email")
    private String shippingEmail;

    @Column(name = "shipping_address1")
    private String shippingAddress1;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "shipping_state")
    private String shippingState;

    @Column(name = "shipping_pincode")
    private String shippingPincode;

    @Column(name = "shipping_country")
    private String shippingCountry;


    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "shiprocket_order_id")
    private String shiprocketOrderId;

    @Column(name = "shiprocket_shipment_id")
    private String shiprocketShipmentId;

    @Column(name = "shiprocket_pushed_at")
    private LocalDateTime shiprocketPushedAt;

    @Column(name = "shiprocket_synced_at")
    private LocalDateTime shiprocketSyncedAt;

    @Column(name = "shipping_address2")
    private String shippingAddress2;

    
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);

        if (this.orderStatus == null) {
            this.orderStatus = "processing";
        }

        if (this.paymentStatus == null) {
            this.paymentStatus = "pending";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }


    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Column(name = "shiprocket_awb_code")
    private String shiprocketAwbCode;

    @Column(name = "shiprocket_courier_name")
    private String shiprocketCourierName;

    @Column(name = "shiprocket_tracking_url")
    private String shiprocketTrackingUrl;

    @Column(name = "shiprocket_status")
    private String shiprocketStatus;

    /** Set when place order applied the inviter 10% referral reward to this order. */
    @Column(name = "referral_inviter_discount_applied")
    private Boolean referralInviterDiscountApplied = false;

}
