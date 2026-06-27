package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.dto.profile.PendingSellerSummary;
import com.ecommerce.adminbackend.dto.profile.ProfileRejectRequest;
import com.ecommerce.adminbackend.dto.profile.ProfileReviewResponse;

import java.util.List;
import java.util.Map;

public interface SellerProfileReviewService {

    List<PendingSellerSummary> listPendingProfiles();

    ProfileReviewResponse approveProfile(Long sellerId);

    ProfileReviewResponse rejectProfile(Long sellerId, ProfileRejectRequest request);

    Map<String, Object> getProfileDetail(Long sellerId);
}
