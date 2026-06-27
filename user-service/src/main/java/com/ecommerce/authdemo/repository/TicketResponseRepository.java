package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Integer> {
    List<TicketResponse> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);
}
