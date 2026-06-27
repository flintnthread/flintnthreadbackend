package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerUniqueIdService {

    private final SellerRepository sellerRepository;

    @Transactional
    public String ensureSellerUniqueId(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        return ensureSellerUniqueId(seller);
    }

    @Transactional
    public String ensureSellerUniqueId(Seller seller) {
        if (seller.getSellerUniqueId() != null && !seller.getSellerUniqueId().isBlank()) {
            return seller.getSellerUniqueId().trim();
        }

        String candidate = formatUniqueId(seller.getId());
        if (!sellerRepository.existsBySellerUniqueIdIgnoreCase(candidate)) {
            seller.setSellerUniqueId(candidate);
            sellerRepository.save(seller);
            return candidate;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            String randomCode = formatUniqueId(seller.getId()) + (int) (Math.random() * 900 + 100);
            if (!sellerRepository.existsBySellerUniqueIdIgnoreCase(randomCode)) {
                seller.setSellerUniqueId(randomCode);
                sellerRepository.save(seller);
                return randomCode;
            }
        }

        throw new IllegalStateException("Unable to generate a unique seller ID.");
    }

    static String formatUniqueId(Long sellerId) {
        return "FNT-SELLER-" + String.format("%06d", sellerId);
    }
}
