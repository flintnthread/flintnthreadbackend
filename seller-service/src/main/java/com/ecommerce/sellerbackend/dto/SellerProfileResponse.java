package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SellerProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String mobile;
    private String businessName;
    private String businessType;
    private String sellerCategory;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String area;
    private String landmark;
    private String gstNumber;
    private String panNumber;
    private String aadhaarNumber;
    private Boolean hasGst;
    private String gstType;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String accountHolder;
    private String branchName;
    private Boolean bankVerified;
    private String profilePic;
    private String profilePicUrl;
    private String aadharFrontUrl;
    private String aadharBackUrl;
    private String panCardUrl;
    private String businessProofUrl;
    private String bankProofUrl;
    private String cancelledChequeUrl;
    private Boolean profileCompleted;
    private Boolean kycCompleted;
    private Boolean kycVerified;
    private BigDecimal walletBalance;
    private String warehouseAddress;
    private String warehouseCity;
    private String warehouseState;
    private String warehouseCountry;
    private String warehouseArea;
    private String status;
    private String referralCode;
    private long referralTotalReferred;
    private long referralQualifiedReferred;
    private int referralGoal;
}
