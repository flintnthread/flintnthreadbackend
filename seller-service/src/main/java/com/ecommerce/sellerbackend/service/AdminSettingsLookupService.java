package com.ecommerce.sellerbackend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminSettingsLookupService {

    public BigDecimal getSellerCommissionPercent() {
        return BigDecimal.ZERO;
    }
}
