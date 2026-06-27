package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
