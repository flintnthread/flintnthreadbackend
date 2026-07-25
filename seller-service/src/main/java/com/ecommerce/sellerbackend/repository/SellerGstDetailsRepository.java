package com.ecommerce.sellerbackend.repository;


import com.ecommerce.sellerbackend.entity.SellerGstDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
    public interface SellerGstDetailsRepository
            extends JpaRepository<SellerGstDetails, Integer> {

        Optional<SellerGstDetails> findBySellerId(Integer sellerId);

        Optional<SellerGstDetails> findByGstin(String gstin);

        Optional<SellerGstDetails> findByGstinIgnoreCase(String gstin);

        boolean existsByGstin(String gstin);

        boolean existsByGstinIgnoreCase(String gstin);
    }

