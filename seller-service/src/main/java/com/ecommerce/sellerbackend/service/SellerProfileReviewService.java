package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.profile.ProfileRejectRequest;
import com.ecommerce.sellerbackend.dto.profile.ProfileReviewResponse;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerKycImageRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellerProfileReviewService {

    private final SellerRepository sellerRepository;
    private final SellerKycImageRepository sellerKycImageRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional
    public ProfileReviewResponse approveProfile(Long sellerId) {
        Seller seller = requirePendingReviewSeller(sellerId);

        LocalDateTime now = LocalDateTime.now();
        seller.setStatus(SellerAccountStatus.active);
        seller.setKycCompleted(true);
        seller.setKycVerified(true);
        seller.setKycVerifiedAt(now);
        seller.setProfileNeedsVerification(false);
        seller.setProfileCompleted(true);
        seller.setAdminRemarks(null);
        seller.setKycRemarks("Profile approved and account activated.");
        seller.setUpdatedAt(now);
        seller.setProfileUpdatedAt(now);
        sellerRepository.save(seller);

        return ProfileReviewResponse.builder()
                .sellerId(sellerId)
                .action("approved")
                .message("Seller profile approved and account activated.")
                .build();
    }

    @Transactional
    public ProfileReviewResponse rejectProfile(Long sellerId, ProfileRejectRequest request) {
        Seller seller = requirePendingReviewSeller(sellerId);

        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Profile rejected by admin.";

        LocalDateTime now = LocalDateTime.now();
        seller.setStatus(SellerAccountStatus.rejected);
        seller.setProfileNeedsVerification(false);
        seller.setKycCompleted(false);
        seller.setKycVerified(false);
        seller.setAdminRemarks(reason);
        seller.setKycRemarks(reason);
        seller.setUpdatedAt(now);
        seller.setProfileUpdatedAt(now);
        sellerRepository.save(seller);

        return ProfileReviewResponse.builder()
                .sellerId(sellerId)
                .action("rejected")
                .message("Seller profile rejected. Reason: " + reason)
                .build();
    }

    private Seller requirePendingReviewSeller(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));

        if (!Boolean.TRUE.equals(seller.getProfileCompleted())) {
            throw new IllegalArgumentException("Seller has not submitted a completed profile yet.");
        }
        if (!Boolean.TRUE.equals(seller.getProfileNeedsVerification())) {
            throw new IllegalArgumentException("Seller profile is not awaiting admin review.");
        }
        return seller;
    }
}
