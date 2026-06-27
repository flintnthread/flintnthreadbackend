package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ActivitySummaryDTO;
import com.ecommerce.authdemo.dto.RecentlyViewedActivityDTO;
import com.ecommerce.authdemo.dto.UserActivityReviewDTO;

import java.util.List;

public interface ActivityService {
    ActivitySummaryDTO getSummary(Long userId, String sessionId, Long authenticatedUserId);

    List<RecentlyViewedActivityDTO> getRecentlyViewed(
            Long userId,
            String sessionId,
            String search,
            Long categoryId,
            String sort,
            String availability
    );

    List<UserActivityReviewDTO> getMyReviews(Long userId);
}
