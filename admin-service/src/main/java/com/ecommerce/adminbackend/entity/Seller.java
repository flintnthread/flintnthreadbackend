package com.ecommerce.adminbackend.entity;

import com.ecommerce.adminbackend.entity.converter.SellerAccountStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@DynamicUpdate
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referral_code", length = 30)
    private String referralCode;

    @Column(name = "referred_by_seller_id")
    private Long referredBySellerId;

    @Column(name = "seller_unique_id", length = 50)
    private String sellerUniqueId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Transient
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String mobile;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "mobile_verified", nullable = false)
    private Boolean mobileVerified = false;

    @Column(name = "mobile_verified_at")
    private LocalDateTime mobileVerifiedAt;

    @Column(name = "otp_sent_at")
    private LocalDateTime otpSentAt;

    @Column(nullable = false)
    private String password;

    @Convert(converter = SellerAccountStatusConverter.class)
    @Column(nullable = false, length = 20)
    private SellerAccountStatus status = SellerAccountStatus.pending;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_type")
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_category", nullable = false, length = 10)
    private SellerCategory sellerCategory = SellerCategory.b2c;

    @Column(name = "company_pan", length = 10)
    private String companyPan;

    @Column(name = "company_pan_doc")
    private String companyPanDoc;

    @Column(name = "incorporation_certificate")
    private String incorporationCertificate;

    @Column(name = "partnership_deed")
    private String partnershipDeed;

    @Column(name = "msme_certificate")
    private String msmeCertificate;

    @Column(name = "iec_certificate")
    private String iecCertificate;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String state;

    private String pincode;

    private String country;

    private String area;

    @Column(name = "road_number")
    private String roadNumber;

    private String landmark;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "has_gst")
    private Boolean hasGst = false;

    @Column(name = "gst_type", length = 50)
    private String gstType;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "bank_verified")
    private Boolean bankVerified;

    @Column(name = "aadhar_front")
    private String aadharFront;

    @Column(name = "aadhar_back")
    private String aadharBack;

    @Column(name = "pan_card")
    private String panCard;

    @Column(name = "business_proof")
    private String businessProof;

    @Column(name = "bank_proof")
    private String bankProof;

    @Column(name = "cancelled_cheque")
    private String cancelledCheque;

    @Column(name = "profile_pic")
    private String profilePic;

    @Column(name = "live_selfie")
    private String liveSelfie;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    @Column(name = "kyc_completed", nullable = false)
    private Boolean kycCompleted = false;

    @Column(name = "kyc_verified", nullable = false)
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_remarks", columnDefinition = "TEXT")
    private String kycRemarks;

    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(name = "wallet_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "warehouse_address", columnDefinition = "TEXT")
    private String warehouseAddress;

    @Column(name = "warehouse_country", length = 100)
    private String warehouseCountry;

    @Column(name = "warehouse_state", length = 100)
    private String warehouseState;

    @Column(name = "warehouse_city", length = 100)
    private String warehouseCity;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;

    @Column(name = "profile_needs_verification")
    private Boolean profileNeedsVerification = false;

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        fullName = buildFullName();
    }

    private String buildFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "Seller" : combined;
    }

    public String getFullName() {
        if (fullName == null || fullName.isBlank()) {
            fullName = buildFullName();
        }
        return fullName;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        fullName = buildFullName();
    }
}
