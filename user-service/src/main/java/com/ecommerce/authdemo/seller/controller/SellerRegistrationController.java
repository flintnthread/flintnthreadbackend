package com.ecommerce.authdemo.seller.controller;

import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.seller.service.SellerRegistrationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
public class SellerRegistrationController {

    private final SellerRegistrationService sellerRegistrationService;

    public SellerRegistrationController(SellerRegistrationService sellerRegistrationService) {
        this.sellerRegistrationService = sellerRegistrationService;
    }

    @PostMapping("/register")
    public Seller registerSeller(@RequestBody Seller seller) {
        return sellerRegistrationService.registerSeller(seller);
    }

    @GetMapping("/test")
    public String test() {
        return "Seller API Working";
    }
}
