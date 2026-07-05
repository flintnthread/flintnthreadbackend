package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.SizeChartRequest;
import com.ecommerce.sellerbackend.dto.SizeChartResponse;
import com.ecommerce.sellerbackend.service.SizeChartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/size-charts")
@RequiredArgsConstructor
public class SizeChartController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SizeChartService sizeChartService;

    @GetMapping
    public List<SizeChartResponse> list(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sizeChartService.listForSeller(requireSellerId(sellerId));
    }

    @GetMapping("/{id}")
    public SizeChartResponse get(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Integer id) {
        return sizeChartService.getForSeller(requireSellerId(sellerId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SizeChartResponse create(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody SizeChartRequest request) {
        return sizeChartService.create(requireSellerId(sellerId), request);
    }

    @PutMapping("/{id}")
    public SizeChartResponse update(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Integer id,
            @Valid @RequestBody SizeChartRequest request) {
        return sizeChartService.update(requireSellerId(sellerId), id, request);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
