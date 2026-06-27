package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSellerProfileRequest {
    private String firstName;
    private String lastName;
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
    private String profilePic;
    private String aadharFront;
    private String aadharBack;
    private String panCard;
    private String businessProof;
    private String bankProof;
    private String cancelledCheque;
    private String warehouseAddress;
    private String warehouseCity;
    private String warehouseState;
    private String warehouseCountry;
    private String warehouseArea;
    private Boolean profileCompleted;
    private Boolean kycCompleted;
}
