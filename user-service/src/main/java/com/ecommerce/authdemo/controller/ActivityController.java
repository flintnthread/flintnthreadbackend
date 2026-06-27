package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ActivitySummaryDTO;
import com.ecommerce.authdemo.dto.RecentlyViewedActivityDTO;
import com.ecommerce.authdemo.dto.UserActivityReviewDTO;
import com.ecommerce.authdemo.service.ActivityService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final SecurityUtil securityUtil;

    @GetMapping("/summary")
    public ActivitySummaryDTO summary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId
    ) {
        Long authUserId = resolveAuthenticatedUserId();
        return activityService.getSummary(userId, sessionId, authUserId);
    }

    @GetMapping("/recently-viewed")
    public List<RecentlyViewedActivityDTO> recentlyViewed(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String availability
    ) {
        return activityService.getRecentlyViewed(userId, sessionId, search, categoryId, sort, availability);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<List<UserActivityReviewDTO>> myReviews() {
        Long userId = resolveAuthenticatedUserId();
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(activityService.getMyReviews(userId));
    }

    private Long resolveAuthenticatedUserId() {
        try {
            return securityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
