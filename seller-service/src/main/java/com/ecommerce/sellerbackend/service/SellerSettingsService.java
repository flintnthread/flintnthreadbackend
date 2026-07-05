package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.SellerRegistrationInvoiceResponse;
import com.ecommerce.sellerbackend.dto.SellerSettingsResponse;
import com.ecommerce.sellerbackend.dto.UpdateSellerSettingsRequest;

import java.util.List;

public interface SellerSettingsService {
    SellerSettingsResponse get(Long sellerId);

    SellerSettingsResponse update(Long sellerId, UpdateSellerSettingsRequest request);

    void deactivateAccount(Long sellerId);

    List<SellerRegistrationInvoiceResponse> listRegistrationInvoices(Long sellerId);

    byte[] downloadRegistrationInvoice(Long sellerId, Long invoiceId, String[] invoiceNumberOut);
}
