package com.ecommerce.authdemo.seller.service;

import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.repository.SellerRepository;
import org.springframework.stereotype.Service;

@Service
public class SellerRegistrationService {

    private final SellerRepository sellerRepository;

    public SellerRegistrationService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    public Seller registerSeller(Seller seller) {
        return sellerRepository.save(seller);
    }
}
