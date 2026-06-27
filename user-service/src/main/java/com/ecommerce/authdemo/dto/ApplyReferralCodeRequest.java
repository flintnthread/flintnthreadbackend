package com.ecommerce.authdemo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyReferralCodeRequest {

    private Integer userId;
    private String referralCode;
}

