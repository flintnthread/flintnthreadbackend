package com.ecommerce.adminbackend.dto.seller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerStatusUpdateRequest {
    /** Account status: pending, active, rejected, inactive */
    private String status;
    /** KYC review state: pending_verification, verified, rejected */
    private String kycVerificationStatus;
    private String kycRemarks;
}
