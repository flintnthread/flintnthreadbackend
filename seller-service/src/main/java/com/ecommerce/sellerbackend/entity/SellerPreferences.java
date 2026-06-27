package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_preferences")
@Getter
@Setter
public class SellerPreferences {

    @Id
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "push_notifications", nullable = false)
    private Boolean pushNotifications = true;

    @Column(name = "order_updates", nullable = false)
    private Boolean orderUpdates = true;

    @Column(name = "payout_alerts", nullable = false)
    private Boolean payoutAlerts = true;

    @Column(name = "vacation_mode", nullable = false)
    private Boolean vacationMode = false;

    @Column(name = "dark_mode", nullable = false)
    private Boolean darkMode = false;

    @Column(name = "language", nullable = false, length = 10)
    private String language = "en-IN";

    @Column(name = "biometric_login", nullable = false)
    private Boolean biometricLogin = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
