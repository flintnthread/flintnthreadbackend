package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.profile.ProfileRejectRequest;
import com.ecommerce.sellerbackend.dto.profile.ProfileReviewResponse;
import com.ecommerce.sellerbackend.service.SellerProfileReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/seller-profiles")
@RequiredArgsConstructor
public class SellerProfileReviewController {

    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final SellerProfileReviewService sellerProfileReviewService;

    @Value("${app.admin.api-key:dev-admin-key}")
    private String adminApiKey;

    @PostMapping("/{sellerId}/approve")
    public ProfileReviewResponse approve(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @PathVariable Long sellerId) {
        verifyAdminKey(adminKey);
        return sellerProfileReviewService.approveProfile(sellerId);
    }

    @PostMapping("/{sellerId}/reject")
    public ProfileReviewResponse reject(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @PathVariable Long sellerId,
            @RequestBody(required = false) ProfileRejectRequest request) {
        verifyAdminKey(adminKey);
        return sellerProfileReviewService.rejectProfile(sellerId, request);
    }

    private void verifyAdminKey(String adminKey) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Admin API key is not configured.");
        }
        if (adminKey == null || !adminApiKey.equals(adminKey.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin API key.");
        }
    }
}
