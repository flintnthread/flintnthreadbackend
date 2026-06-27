package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.SellerSettingsResponse;
import com.ecommerce.sellerbackend.dto.UpdateSellerSettingsRequest;

public interface SellerSettingsService {
    SellerSettingsResponse get(Long sellerId);

    SellerSettingsResponse update(Long sellerId, UpdateSellerSettingsRequest request);

    void deactivateAccount(Long sellerId);
}
