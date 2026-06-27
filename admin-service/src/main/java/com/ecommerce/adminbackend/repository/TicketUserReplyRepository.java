package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.TicketUserReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketUserReplyRepository extends JpaRepository<TicketUserReply, Integer> {

    List<TicketUserReply> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);
}
