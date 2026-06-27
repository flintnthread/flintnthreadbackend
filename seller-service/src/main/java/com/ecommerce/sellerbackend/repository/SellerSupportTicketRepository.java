package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerSupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerSupportTicketRepository extends JpaRepository<SellerSupportTicket, Long> {

    List<SellerSupportTicket> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<SellerSupportTicket> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, String status);

    List<SellerSupportTicket> findBySellerIdAndCategoryOrderByCreatedAtDesc(Long sellerId, String category);

    Optional<SellerSupportTicket> findByTicketNumber(String ticketNumber);

    Optional<SellerSupportTicket> findByIdAndSellerId(Long id, Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, String status);
}
