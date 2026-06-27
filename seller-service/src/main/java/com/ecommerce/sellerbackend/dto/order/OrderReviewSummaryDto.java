package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderReviewSummaryDto {
    private int returnCount;
    private int exchangeCount;
    private int replacementCount;
    private int cancellationCount;
    private boolean hasPendingReview;
}
