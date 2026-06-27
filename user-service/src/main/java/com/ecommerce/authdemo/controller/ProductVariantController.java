package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.StockResponse;
import com.ecommerce.authdemo.service.ProductVariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/variant")
public class ProductVariantController {

    @Autowired
    private ProductVariantService service;

    @GetMapping("/stock/{variantId}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long variantId) {
        return ResponseEntity.ok(service.getStockByVariantId(variantId));



    }



    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getVariantsByProduct(
            @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
                service.getVariantsByProductId(productId)
        );
    }
}