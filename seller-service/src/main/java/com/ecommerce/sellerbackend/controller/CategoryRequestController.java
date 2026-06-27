package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.CategoryRequestPayload;
import com.ecommerce.sellerbackend.dto.CategoryRequestResponse;
import com.ecommerce.sellerbackend.service.CategoryRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/category-requests")
@RequiredArgsConstructor
public class CategoryRequestController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final CategoryRequestService categoryRequestService;

    @GetMapping
    public List<CategoryRequestResponse> list(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return categoryRequestService.listForSeller(requireSellerId(sellerId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRequestResponse create(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody CategoryRequestPayload payload) {
        return categoryRequestService.create(requireSellerId(sellerId), payload);
    }

    @PutMapping("/{requestId}")
    public CategoryRequestResponse update(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long requestId,
            @Valid @RequestBody CategoryRequestPayload payload) {
        return categoryRequestService.update(requireSellerId(sellerId), requestId, payload);
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long requestId) {
        categoryRequestService.delete(requireSellerId(sellerId), requestId);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
