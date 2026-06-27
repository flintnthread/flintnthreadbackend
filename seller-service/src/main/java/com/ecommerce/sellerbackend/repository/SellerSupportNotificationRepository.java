package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerSupportNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerSupportNotificationRepository extends JpaRepository<SellerSupportNotification, Long> {

    List<SellerSupportNotification> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<SellerSupportNotification> findBySellerIdAndIsReadFalseOrderByCreatedAtDesc(Long sellerId);

    long countBySellerIdAndIsReadFalse(Long sellerId);
}
