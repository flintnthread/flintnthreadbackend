package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferralRewardStatusDto {

    private boolean rewardUnlocked;
    private boolean discountAvailable;
    /** True when the next qualifying paid order can apply the 10% inviter discount. */
    private boolean eligibleForInviterDiscountOnNextOrder;
    private int percentOff;
    private String message;
}
