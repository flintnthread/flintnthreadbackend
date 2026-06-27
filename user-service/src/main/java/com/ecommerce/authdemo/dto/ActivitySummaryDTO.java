package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivitySummaryDTO {
    private int recentlyViewedCount;
    private int wishlistCount;
    private int reviewsCount;
}
