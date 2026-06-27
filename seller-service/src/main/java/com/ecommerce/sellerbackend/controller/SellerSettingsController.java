package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.SellerSettingsResponse;
import com.ecommerce.sellerbackend.dto.UpdateSellerSettingsRequest;
import com.ecommerce.sellerbackend.service.SellerSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/settings")
@RequiredArgsConstructor
public class SellerSettingsController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SellerSettingsService sellerSettingsService;

    @GetMapping
    public SellerSettingsResponse get(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerSettingsService.get(requireSellerId(sellerId));
    }

    @PutMapping
    public SellerSettingsResponse update(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody UpdateSellerSettingsRequest request) {
        return sellerSettingsService.update(requireSellerId(sellerId), request);
    }

    @PostMapping("/account/deactivate")
    public Map<String, String> deactivateAccount(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        sellerSettingsService.deactivateAccount(requireSellerId(sellerId));
        return Map.of("message", "Your seller account has been deactivated.");
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
