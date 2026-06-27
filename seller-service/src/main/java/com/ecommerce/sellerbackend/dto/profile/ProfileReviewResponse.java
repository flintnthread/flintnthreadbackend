package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileReviewResponse {
    private final Long sellerId;
    private final String action;
    private final String message;
}
