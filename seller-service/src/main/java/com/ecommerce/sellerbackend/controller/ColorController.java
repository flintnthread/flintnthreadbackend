package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.ColorRequest;
import com.ecommerce.sellerbackend.dto.ColorResponse;
import com.ecommerce.sellerbackend.service.ColorService;
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
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final ColorService colorService;

    @GetMapping
    public List<ColorResponse> list(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return colorService.listForSeller(requireSellerId(sellerId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColorResponse create(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody ColorRequest request) {
        return colorService.create(requireSellerId(sellerId), request);
    }

    @PutMapping("/{id}")
    public ColorResponse update(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id,
            @Valid @RequestBody ColorRequest request) {
        return colorService.update(requireSellerId(sellerId), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id) {
        colorService.delete(requireSellerId(sellerId), id);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
