package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReferralCodeService {

    private final SellerRepository sellerRepository;

    @Transactional
    public String ensureReferralCode(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        if (seller.getReferralCode() != null && !seller.getReferralCode().isBlank()) {
            String existing = seller.getReferralCode().trim();
            if (existing.startsWith("FT") && !existing.startsWith("F&T")) {
                String migrated = "F&T" + existing.substring(2);
                if (!sellerRepository.existsByReferralCodeIgnoreCase(migrated)) {
                    seller.setReferralCode(migrated);
                    sellerRepository.save(seller);
                    return migrated;
                }
            }
            return existing;
        }

        String candidate = formatReferralCandidate(sellerId);
        if (!sellerRepository.existsByReferralCodeIgnoreCase(candidate)) {
            seller.setReferralCode(candidate);
            sellerRepository.save(seller);
            return candidate;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            String randomCode = formatReferralCandidate(sellerId) + (int) (Math.random() * 900 + 100);
            if (!sellerRepository.existsByReferralCodeIgnoreCase(randomCode)) {
                seller.setReferralCode(randomCode);
                sellerRepository.save(seller);
                return randomCode;
            }
        }

        throw new IllegalStateException("Unable to generate a unique referral code.");
    }

    private static String formatReferralCandidate(Long sellerId) {
        return "F&T" + String.format("%05d", sellerId);
    }
}
