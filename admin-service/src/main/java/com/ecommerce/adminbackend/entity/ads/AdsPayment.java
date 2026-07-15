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
@Table(name = "ads_payments")
@Getter
@Setter
public class AdsPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "razorpay_payment_id", nullable = false, length = 100, unique = true)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id", nullable = false, length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_signature", nullable = false)
    private String razorpaySignature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 50)
    private String method;

    @Column(length = 100)
    private String bank;

    @Column(length = 100)
    private String wallet;

    @Column(length = 100)
    private String vpa;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String contact;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
