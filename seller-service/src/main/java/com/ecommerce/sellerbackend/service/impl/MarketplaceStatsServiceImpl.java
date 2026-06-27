package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.MarketplaceStatsResponse;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.repository.MarketplaceStatsRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.MarketplaceStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketplaceStatsServiceImpl implements MarketplaceStatsService {

    private static final int DEFAULT_APPROVAL_HOURS = 48;

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final MarketplaceStatsRepository marketplaceStatsRepository;

    @Override
    @Transactional(readOnly = true)
    public MarketplaceStatsResponse getPublicStats() {
        long sellersCount = sellerRepository.countByStatus(SellerAccountStatus.active);
        long productsCount = productRepository.countApprovedProducts();
        long customersCount = marketplaceStatsRepository.countDistinctCustomers();

        int approvalHours = resolveApprovalHours(marketplaceStatsRepository.averageApprovalHours());

        return MarketplaceStatsResponse.builder()
                .sellersCount(sellersCount)
                .productsCount(productsCount)
                .customersCount(customersCount)
                .avgApprovalHours(approvalHours)
                .sellersDisplay(formatCompactCount(sellersCount))
                .productsDisplay(formatCompactCount(productsCount))
                .customersDisplay(formatCompactCount(customersCount))
                .approvalDisplay(formatApprovalHours(approvalHours))
                .build();
    }

    private static int resolveApprovalHours(Double averageHours) {
        if (averageHours == null || averageHours.isNaN() || averageHours <= 0) {
            return DEFAULT_APPROVAL_HOURS;
        }
        return Math.max(1, (int) Math.round(averageHours));
    }

    static String formatCompactCount(long count) {
        if (count <= 0) {
            return "0";
        }
        if (count >= 1_000_000) {
            long millions = count / 1_000_000;
            return millions + "M+";
        }
        if (count >= 10_000) {
            long thousands = count / 1_000;
            return thousands + "K+";
        }
        if (count >= 1_000) {
            double thousands = count / 1_000.0;
            String formatted = String.format("%.1f", thousands).replace(".0", "");
            return formatted + "K+";
        }
        if (count >= 100) {
            long rounded = (count / 100) * 100;
            return rounded + "+";
        }
        return count + "+";
    }

    static String formatApprovalHours(int hours) {
        if (hours <= 0) {
            return DEFAULT_APPROVAL_HOURS + "hr";
        }
        return hours + "hr";
    }
}
