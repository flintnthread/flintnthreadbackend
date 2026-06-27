package com.ecommerce.sellerbackend.util;

import com.ecommerce.sellerbackend.dto.profile.SellerAccountStatusResponse;
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;

public final class SellerAccountStatusHelper {

    public static final int REVIEW_ESTIMATE_HOURS = 24;

    private SellerAccountStatusHelper() {
    }

    public static SellerAccountStatusResponse build(Seller seller) {
        boolean profileCompleted = Boolean.TRUE.equals(seller.getProfileCompleted());
        boolean kycCompleted = Boolean.TRUE.equals(seller.getKycCompleted())
                || Boolean.TRUE.equals(seller.getKycVerified());
        boolean awaitingReview = Boolean.TRUE.equals(seller.getProfileNeedsVerification());
        SellerAccountStatus status = seller.getStatus() != null
                ? seller.getStatus()
                : SellerAccountStatus.pending;

        String profileLabel = profileCompleted ? "Complete" : "Incomplete";
        String kycLabel = kycCompleted ? "Verified" : (status == SellerAccountStatus.rejected ? "Rejected" : "Pending");
        String rejectionReason = trimToNull(seller.getAdminRemarks());
        if (rejectionReason == null) {
            rejectionReason = trimToNull(seller.getKycRemarks());
        }

        if (status == SellerAccountStatus.rejected) {
            String reasonText = rejectionReason != null
                    ? rejectionReason
                    : "Your profile did not meet our verification requirements.";
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("rejected")
                    .title("Rejected")
                    .message("Your seller profile was not approved. " + reasonText
                            + " Please update your documents and contact support if you need assistance.")
                    .profileLabel(profileLabel)
                    .kycLabel("Rejected")
                    .canManageProducts(false)
                    .canReceiveOrders(false)
                    .reviewEstimateHours(null)
                    .rejectionReason(rejectionReason)
                    .build();
        }

        if (status == SellerAccountStatus.suspended) {
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("suspended")
                    .title("Suspended")
                    .message("Your seller account has been suspended. Please contact support for assistance.")
                    .profileLabel(profileLabel)
                    .kycLabel(kycLabel)
                    .canManageProducts(false)
                    .canReceiveOrders(false)
                    .reviewEstimateHours(null)
                    .rejectionReason(null)
                    .build();
        }

        if (status == SellerAccountStatus.inactive) {
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("inactive")
                    .title("Inactive")
                    .message("Your seller account is currently inactive. Please contact support to reactivate.")
                    .profileLabel(profileLabel)
                    .kycLabel(kycLabel)
                    .canManageProducts(false)
                    .canReceiveOrders(false)
                    .reviewEstimateHours(null)
                    .rejectionReason(null)
                    .build();
        }

        if (!profileCompleted) {
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("profile_incomplete")
                    .title("Profile Incomplete")
                    .message("Complete your seller profile and submit your documents to begin the verification process.")
                    .profileLabel("Incomplete")
                    .kycLabel("Not submitted")
                    .canManageProducts(false)
                    .canReceiveOrders(false)
                    .reviewEstimateHours(null)
                    .rejectionReason(null)
                    .build();
        }

        if (awaitingReview || status == SellerAccountStatus.pending) {
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("pending_review")
                    .title("Pending Review")
                    .message("Your profile has been submitted and is currently under review by our team. "
                            + "Please allow up to " + REVIEW_ESTIMATE_HOURS + " hours for approval. "
                            + "You will be notified once your account is activated.")
                    .profileLabel("Complete")
                    .kycLabel("Pending")
                    .canManageProducts(false)
                    .canReceiveOrders(false)
                    .reviewEstimateHours(REVIEW_ESTIMATE_HOURS)
                    .rejectionReason(null)
                    .build();
        }

        if (status == SellerAccountStatus.active) {
            return SellerAccountStatusResponse.builder()
                    .status(status.name())
                    .approvalState("approved")
                    .title("Active")
                    .message("Your account is fully verified and active. You can manage products and receive orders.")
                    .profileLabel("Complete")
                    .kycLabel(kycCompleted ? "Verified" : "Pending")
                    .canManageProducts(true)
                    .canReceiveOrders(true)
                    .reviewEstimateHours(null)
                    .rejectionReason(null)
                    .build();
        }

        return SellerAccountStatusResponse.builder()
                .status(status.name())
                .approvalState("pending_review")
                .title("Pending")
                .message("Your account status is being processed. Please check back shortly or contact support.")
                .profileLabel(profileLabel)
                .kycLabel(kycLabel)
                .canManageProducts(false)
                .canReceiveOrders(false)
                .reviewEstimateHours(REVIEW_ESTIMATE_HOURS)
                .rejectionReason(null)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
