package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.TicketAdminResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAdminResponseRepository extends JpaRepository<TicketAdminResponse, Integer> {

    List<TicketAdminResponse> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);
}
