package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerLiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerLiveChatMessageRepository extends JpaRepository<SellerLiveChatMessage, Long> {

    List<SellerLiveChatMessage> findBySellerIdOrderByCreatedAtAsc(Integer sellerId);
}
