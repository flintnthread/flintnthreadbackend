package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches mobile app: referralCode, confirmedReferrals, requiredReferrals, rewardUnlocked,
 * alreadyUsedReferral (joined with a friend's code).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReferralDashboardDto {

    /** Resolved from JWT on the server; use to confirm the correct account. */
    private Long userId;

    private String referralCode;
    private int confirmedReferrals;
    private int requiredReferrals;
    private int remainingReferrals;
    private boolean rewardUnlocked;
    private boolean alreadyUsedReferral;
    private boolean discountAvailable;
    private String reward;
}
