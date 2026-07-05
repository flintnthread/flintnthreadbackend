package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.financial.ShiprocketSyncResponse;
import com.ecommerce.sellerbackend.dto.order.SellerOrderDetailDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderStatsDto;
import com.ecommerce.sellerbackend.dto.order.SellerOrderSummaryDto;
import com.ecommerce.sellerbackend.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.sellerbackend.service.OrderService;
import com.ecommerce.sellerbackend.service.SellerFinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class OrderController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final OrderService orderService;
    private final SellerFinancialService sellerFinancialService;

    @GetMapping
    public List<SellerOrderSummaryDto> list(
            @RequestHeader(value = SELLER_ID_HEADER, required = false) Long sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) Long sellerIdQuery) {
        return orderService.listForSeller(requireSellerId(resolveSellerId(sellerIdHeader, sellerIdQuery)));
    }

    @GetMapping("/details")
    public List<SellerOrderDetailDto> listDetails(
            @RequestHeader(value = SELLER_ID_HEADER, required = false) Long sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) Long sellerIdQuery) {
        return orderService.listDetailsForSeller(requireSellerId(resolveSellerId(sellerIdHeader, sellerIdQuery)));
    }

    @GetMapping("/stats")
    public SellerOrderStatsDto stats(
            @RequestHeader(value = SELLER_ID_HEADER, required = false) Long sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) Long sellerIdQuery) {
        return orderService.statsForSeller(requireSellerId(resolveSellerId(sellerIdHeader, sellerIdQuery)));
    }

    @GetMapping("/{orderKey}")
    public SellerOrderDetailDto get(
            @RequestHeader(value = SELLER_ID_HEADER, required = false) Long sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) Long sellerIdQuery,
            @PathVariable String orderKey) {
        return orderService.getForSeller(
                requireSellerId(resolveSellerId(sellerIdHeader, sellerIdQuery)),
                orderKey);
    }

    @PatchMapping("/{orderKey}/status")
    public SellerOrderDetailDto updateStatus(
            @RequestHeader(value = SELLER_ID_HEADER, required = false) Long sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) Long sellerIdQuery,
            @PathVariable String orderKey,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatusForSeller(
                requireSellerId(resolveSellerId(sellerIdHeader, sellerIdQuery)),
                orderKey,
                request.getStatus(),
                request.getComment());
    }

    @PostMapping("/{orderKey}/shiprocket/sync")
    public ShiprocketSyncResponse syncShiprocket(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable String orderKey) {
        return sellerFinancialService.syncShiprocket(requireSellerId(sellerId), orderKey);
    }

    private Long resolveSellerId(Long sellerIdHeader, Long sellerIdQuery) {
        return sellerIdHeader != null ? sellerIdHeader : sellerIdQuery;
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
