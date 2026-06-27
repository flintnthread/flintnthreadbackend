package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ReferralDashboardDto;
import com.ecommerce.authdemo.dto.ReferralResponse;
import com.ecommerce.authdemo.dto.ReferralRewardStatusDto;
import com.ecommerce.authdemo.dto.ShareDto;

import java.math.BigDecimal;

public interface ReferralService {

    /** Referrals required before inviter unlocks 10% off first order. */
    int getRequiredReferralsForReward();

    void generateCodes(Long userId, String username);

    ReferralResponse applyReferral(Long userId, String referralCode);

    BigDecimal calculateFirstOrderDiscount(Long userId, BigDecimal subtotal);

    ReferralDashboardDto getDashboard(Long userId);

    String refreshReferralCode(Long userId);

    ShareDto getShareData(Long userId);

    void markReferralDiscountUsed(Long userId, Long orderId);

    void confirmReferralOnFirstPaidOrder(Long userId);

    BigDecimal getAvailableReferralDiscountPercentForUser(Long userId);

    ReferralRewardStatusDto getRewardStatus(Long userId);
}