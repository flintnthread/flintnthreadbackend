package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerBankEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerBankEditRequestRepository extends JpaRepository<SellerBankEditRequest, Long> {

    List<SellerBankEditRequest> findBySellerIdOrderByRequestedAtDesc(Long sellerId);

    List<SellerBankEditRequest> findByStatusOrderByRequestedAtDesc(String status);

    long countBySellerIdAndStatusIgnoreCase(Long sellerId, String status);
}
