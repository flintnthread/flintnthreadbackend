package com.ecommerce.sellerbackend.controller;


import com.ecommerce.sellerbackend.service.SellerGstDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
    @RequestMapping("/api/seller/gst")
    @RequiredArgsConstructor
    public class SellerGstDetailsController {

        private final SellerGstDetailsService service;

        @GetMapping("/{sellerId}")
        public ResponseEntity<?> getDetails(
                @PathVariable Integer sellerId) {

            return ResponseEntity.ok(
                    service.getBySellerId(sellerId));
        }
    }

