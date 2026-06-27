package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IfscLookupResponse {
    private final String ifscCode;
    private final String bankName;
    private final String branchName;
    private final boolean found;
}
