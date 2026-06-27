package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardReferralDto {
    private final String referralCode;
    private final long totalReferred;
    private final long qualifiedReferred;
    private final int goal;
}
