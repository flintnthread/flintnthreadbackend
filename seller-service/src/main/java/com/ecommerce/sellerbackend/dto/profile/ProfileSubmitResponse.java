package com.ecommerce.sellerbackend.dto.profile;

import com.ecommerce.sellerbackend.dto.profile.SellerAccountStatusResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfileSubmitResponse {
    private final boolean submitted;
    private final boolean profileCompleted;
    private final String message;
    private final List<String> errors;
    private final SellerAccountStatusResponse accountStatus;
}
