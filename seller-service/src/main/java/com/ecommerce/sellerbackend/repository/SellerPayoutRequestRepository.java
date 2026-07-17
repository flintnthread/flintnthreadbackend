package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerPayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerPayoutRequestRepository extends JpaRepository<SellerPayoutRequest, Long> {

    List<SellerPayoutRequest> findBySellerIdOrderByRequestedAtDesc(Long sellerId);

    long countBySellerIdAndStatusIgnoreCase(Long sellerId, String status);

    Optional<SellerPayoutRequest> findFirstBySellerIdAndOrderIdOrderByRequestedAtDesc(Long sellerId, Long orderId);
}
