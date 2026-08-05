package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.service.SellerAccountLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/account/lifecycle")
@RequiredArgsConstructor
public class SellerAccountLifecycleController {

    private static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SellerAccountLifecycleService lifecycleService;

    @GetMapping("/eligibility")
    public Map<String, Object> eligibility(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return lifecycleService.getEligibility(requireSellerId(sellerId));
    }

    @GetMapping("/status")
    public Map<String, Object> status(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return lifecycleService.getStatus(requireSellerId(sellerId));
    }

    @PostMapping("/deactivation/request")
    public Map<String, Object> requestDeactivation(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String duration = body != null && body.get("duration") != null
                ? String.valueOf(body.get("duration"))
                : null;
        return lifecycleService.requestDeactivation(requireSellerId(sellerId), duration);
    }

    @PostMapping("/activation/request")
    public Map<String, Object> requestActivation(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return lifecycleService.requestActivation(requireSellerId(sellerId));
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Seller id is required.");
        }
        return sellerId;
    }
}
