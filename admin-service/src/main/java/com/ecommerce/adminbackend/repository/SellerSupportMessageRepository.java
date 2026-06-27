package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.SellerSupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerSupportMessageRepository extends JpaRepository<SellerSupportMessage, Long> {

    List<SellerSupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
