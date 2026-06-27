package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerSupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerSupportMessageRepository extends JpaRepository<SellerSupportMessage, Long> {

    List<SellerSupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    long countByTicketId(Long ticketId);
}
