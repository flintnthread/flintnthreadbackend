package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerAccountStatusResponse {

    /** Raw seller status enum value, e.g. pending, active, rejected. */
    private final String status;

    /** UI-friendly approval state: profile_incomplete, pending_review, approved, rejected, suspended, inactive. */
    private final String approvalState;

    /** Short headline for dashboard banner, e.g. "Pending Review". */
    private final String title;

    /** Full professional message shown to the seller. */
    private final String message;

    /** Profile step label, e.g. Complete / Incomplete. */
    private final String profileLabel;

    /** KYC label, e.g. Pending / Verified / Rejected. */
    private final String kycLabel;

    /** Whether the seller can list and manage products. */
    private final boolean canManageProducts;

    /** Whether the seller can receive and fulfil orders. */
    private final boolean canReceiveOrders;

    /** Estimated admin review window in hours (shown when pending). */
    private final Integer reviewEstimateHours;

    /** Admin rejection reason, when status is rejected. */
    private final String rejectionReason;
}
