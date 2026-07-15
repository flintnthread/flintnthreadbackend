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
@Table(name = "ads_admin_notifications")
@Getter
@Setter
public class AdsAdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "ad_name", nullable = false)
    private String adName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "billing_type", length = 20)
    private String billingType = "monthly";

    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "monthly_rate", precision = 10, scale = 2)
    private BigDecimal monthlyRate;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
