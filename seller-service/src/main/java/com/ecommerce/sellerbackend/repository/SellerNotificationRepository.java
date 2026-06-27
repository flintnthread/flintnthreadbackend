package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerNotificationRepository extends JpaRepository<SellerNotification, Long> {

    List<SellerNotification> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    long countBySellerIdAndReadFalse(Long sellerId);
}
