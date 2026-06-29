package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.EarningsResponse;
import com.ecommerce.sellerbackend.dto.OrderPayoutAmountDto;
import com.ecommerce.sellerbackend.dto.PayoutRequestBody;
import com.ecommerce.sellerbackend.dto.PayoutRequestResponse;
import com.ecommerce.sellerbackend.dto.PayoutTransactionResponse;
import com.ecommerce.sellerbackend.service.EarningsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/earnings")
@RequiredArgsConstructor
public class EarningsController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final EarningsService earningsService;

    @GetMapping
    public EarningsResponse get(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return earningsService.getEarnings(requireSellerId(sellerId));
    }

    @GetMapping("/payouts")
    public List<PayoutTransactionResponse> payouts(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return earningsService.getPayouts(requireSellerId(sellerId));
    }

    @PostMapping("/payout-request")
    public PayoutRequestResponse payoutRequest(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody PayoutRequestBody body) {
        return earningsService.requestPayout(requireSellerId(sellerId), body);
    }

    @GetMapping("/order-lookup/{orderKey}")
    public OrderPayoutAmountDto orderLookup(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable String orderKey) {
        return earningsService.lookupOrderPayoutAmount(requireSellerId(sellerId), orderKey);
    }




    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
