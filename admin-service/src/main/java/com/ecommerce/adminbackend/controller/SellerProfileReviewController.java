package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.dto.profile.PendingSellerSummary;
import com.ecommerce.adminbackend.dto.profile.ProfileRejectRequest;
import com.ecommerce.adminbackend.dto.profile.ProfileReviewResponse;
import com.ecommerce.adminbackend.service.SellerProfileReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/seller-profiles")
@RequiredArgsConstructor
public class SellerProfileReviewController {

    private static final Logger log = LogFactory.getLogger(SellerProfileReviewController.class);

    private final SellerProfileReviewService sellerProfileReviewService;

    @GetMapping("/pending")
    public List<PendingSellerSummary> listPending() {
        return sellerProfileReviewService.listPendingProfiles();
    }

    @GetMapping("/{sellerId}")
    public Map<String, Object> getProfileDetail(@PathVariable Long sellerId) {
        return sellerProfileReviewService.getProfileDetail(sellerId);
    }

    @PostMapping("/{sellerId}/approve")
    public ProfileReviewResponse approve(@PathVariable Long sellerId) {
        return sellerProfileReviewService.approveProfile(sellerId);
    }

    @PostMapping("/{sellerId}/reject")
    public ProfileReviewResponse reject(
            @PathVariable Long sellerId,
            @RequestBody(required = false) ProfileRejectRequest request) {
        return sellerProfileReviewService.rejectProfile(sellerId, request);
    }
}
