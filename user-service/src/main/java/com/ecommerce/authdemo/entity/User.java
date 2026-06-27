package com.ecommerce.authdemo.entity;

import com.ecommerce.authdemo.dto.Enum.Role;
import com.ecommerce.authdemo.dto.Enum.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Basic Info
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "contact_number")
    private String contactNumber;

    // 🔐 Auth
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean verified = false;

    // 🔹 Profile (NEW for mobile)
    @Column(name = "profile_image")
    private String profileImage;

    @Transient
    private java.time.LocalDate dateOfBirth;

    @Transient
    private String gender;

    // 🔹 Status (FIXED)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.active;

    // 🔹 Optional (future ready)
    @Column(name = "company_reference_id", unique = true)
    private String companyReferenceId;

    // 🔹 Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expo_push_token")
    private String expoPushToken;

    // 🔁 Auto timestamps
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "referred_by")
    private Long referredBy;

    @Column(name = "referral_count")
    private Integer referralCount = 0;

    @Column(name = "reward_unlocked")
    private Boolean rewardUnlocked = false;

    /** True after 5 referrals until the inviter uses the 10% reward on a qualifying order. */
    @Column(name = "discount_available")
    private Boolean discountAvailable = false;

    /** Inviter has used the one-time 10% referral reward on checkout. */
    @Column(name = "first_order_completed")
    private Boolean firstOrderCompleted = false;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}