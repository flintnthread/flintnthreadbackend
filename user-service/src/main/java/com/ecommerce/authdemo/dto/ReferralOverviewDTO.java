package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralOverviewDTO {
    private String referralCode;
    private int confirmedReferrals;
    private int requiredReferrals;
    private double discountPercent;
    private boolean rewardUnlocked;
    private boolean rewardUsed;
}

