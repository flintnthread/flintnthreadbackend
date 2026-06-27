package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class SellerProfileDTO {
    private Long id;
    private String displayName;
    private String businessName;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String branchName;
    private String address;
    private String state;
    private String pincode;
    private String categoryLabel;
    private String joinedLabel;
    private String locationLabel;
    private String shipsToLabel;
    private boolean verified;
    private boolean preferredSeller;
}
