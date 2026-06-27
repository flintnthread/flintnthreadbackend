package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.dto.profile.PendingSellerSummary;
import com.ecommerce.adminbackend.dto.profile.ProfileRejectRequest;
import com.ecommerce.adminbackend.dto.profile.ProfileReviewResponse;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerAccountStatus;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.SellerProfileReviewService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerProfileReviewServiceImpl extends BaseAdminService implements SellerProfileReviewService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM, yyyy hh:mm a");

    private final SellerRepository sellerRepository;
    private final MediaUrlHelper mediaUrlHelper;

    @Override
    @Transactional(readOnly = true)
    public List<PendingSellerSummary> listPendingProfiles() {
        return sellerRepository.findPendingProfileReviews().stream()
                .map(this::toPendingSummary)
                .toList();
    }

    @Override
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

    @Override
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
        Seller seller = requireFound(sellerRepository.findById(sellerId), "Seller not found.");
        if (!Boolean.TRUE.equals(seller.getProfileCompleted())) {
            throw new IllegalArgumentException("Seller has not submitted a completed profile yet.");
        }
        if (!Boolean.TRUE.equals(seller.getProfileNeedsVerification())) {
            throw new IllegalArgumentException("Seller profile is not awaiting admin review.");
        }
        return seller;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProfileDetail(Long sellerId) {
        Seller seller = requireFound(sellerRepository.findById(sellerId), "Seller not found.");
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("sellerId", seller.getId());
        detail.put("fullName", seller.getFullName());
        detail.put("firstName", seller.getFirstName());
        detail.put("lastName", seller.getLastName());
        detail.put("email", seller.getEmail());
        detail.put("mobile", seller.getMobile());
        detail.put("status", seller.getStatus() != null ? seller.getStatus().name() : "pending");
        detail.put("businessName", seller.getBusinessName());
        detail.put("businessType", seller.getBusinessType());
        detail.put("sellerCategory", seller.getSellerCategory() != null ? seller.getSellerCategory().name() : null);
        detail.put("profileUpdatedAt", seller.getProfileUpdatedAt());
        detail.put("createdAt", seller.getCreatedAt());
        detail.put("registeredOn", seller.getProfileUpdatedAt() != null
                ? DISPLAY_DATE_TIME.format(seller.getProfileUpdatedAt())
                : (seller.getCreatedAt() != null ? DISPLAY_DATE_TIME.format(seller.getCreatedAt()) : null));
        detail.put("address", seller.getAddress());
        detail.put("city", seller.getCity());
        detail.put("state", seller.getState());
        detail.put("pincode", seller.getPincode());
        detail.put("country", seller.getCountry());
        detail.put("gstNumber", seller.getGstNumber());
        detail.put("hasGst", seller.getHasGst());
        detail.put("panNumber", seller.getPanNumber());
        detail.put("bankName", seller.getBankName());
        detail.put("branchName", seller.getBranchName());
        detail.put("accountNumber", seller.getAccountNumber());
        detail.put("ifscCode", seller.getIfscCode());
        detail.put("accountHolder", seller.getAccountHolder());
        detail.put("warehouseAddress", seller.getWarehouseAddress());
        detail.put("warehouseCountry", seller.getWarehouseCountry());
        detail.put("warehouseState", seller.getWarehouseState());
        detail.put("warehouseCity", seller.getWarehouseCity());
        detail.put("warehouseArea", seller.getWarehouseArea());
        detail.put("profilePicUrl", mediaUrlHelper.toPublicUrl(seller.getProfilePic()));
        detail.put("profileNeedsVerification", seller.getProfileNeedsVerification());
        detail.put("documents", buildDocuments(seller));
        return detail;
    }

    private List<Map<String, Object>> buildDocuments(Seller seller) {
        List<Map<String, Object>> docs = new ArrayList<>();
        addDoc(docs, "Aadhaar Front", seller.getAadharFront());
        addDoc(docs, "Aadhaar Back", seller.getAadharBack());
        addDoc(docs, "PAN Card", seller.getPanCard());
        addDoc(docs, "Cancelled Cheque", seller.getCancelledCheque());
        addDoc(docs, "Business Proof", seller.getBusinessProof());
        addDoc(docs, "Bank Proof", seller.getBankProof());
        addDoc(docs, "MSME Certificate", seller.getMsmeCertificate());
        addDoc(docs, "Live Selfie", seller.getLiveSelfie());
        return docs;
    }

    private void addDoc(List<Map<String, Object>> docs, String name, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("name", name);
        doc.put("url", mediaUrlHelper.toPublicUrl(path));
        docs.add(doc);
    }

    private PendingSellerSummary toPendingSummary(Seller seller) {
        return PendingSellerSummary.builder()
                .sellerId(seller.getId())
                .fullName(seller.getFullName())
                .email(seller.getEmail())
                .mobile(seller.getMobile())
                .businessName(seller.getBusinessName())
                .status(seller.getStatus() != null ? seller.getStatus().name() : "pending")
                .profileUpdatedAt(seller.getProfileUpdatedAt())
                .build();
    }
}
