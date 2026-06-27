package com.ecommerce.adminbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PendingSellerSummary {
    private final Long sellerId;
    private final String fullName;
    private final String email;
    private final String mobile;
    private final String businessName;
    private final String status;
    private final LocalDateTime profileUpdatedAt;
}
