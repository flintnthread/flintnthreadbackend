package com.ecommerce.adminbackend.entity.ads;

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
@Table(name = "ads_orders")
@Getter
@Setter
public class AdsOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false, length = 50, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "ad_type", nullable = false, length = 50)
    private String adType;

    @Column(name = "ad_id", nullable = false)
    private Integer adId;

    @Column(name = "ad_name", nullable = false)
    private String adName;

    @Column(name = "ad_description", columnDefinition = "TEXT")
    private String adDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "billing_type", length = 20)
    private String billingType = "monthly";

    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "monthly_rate", precision = 10, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "selected_plan", length = 100)
    private String selectedPlan;

    @Column(length = 3)
    private String currency = "INR";

    @Column(length = 20)
    private String status = "pending";

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
