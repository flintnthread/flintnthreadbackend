package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.SellerNotificationResponse;
import com.ecommerce.sellerbackend.service.SellerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SellerNotificationController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SellerNotificationService sellerNotificationService;

    @GetMapping
    public List<SellerNotificationResponse> list(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerNotificationService.listForSeller(requireSellerId(sellerId));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id) {
        sellerNotificationService.markRead(requireSellerId(sellerId), id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        sellerNotificationService.markAllRead(requireSellerId(sellerId));
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
