package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.MarketplaceStatsResponse;
import com.ecommerce.sellerbackend.service.MarketplaceStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicMarketplaceController {

    private final MarketplaceStatsService marketplaceStatsService;

    @GetMapping("/marketplace-stats")
    public MarketplaceStatsResponse marketplaceStats() {
        return marketplaceStatsService.getPublicStats();
    }
}
