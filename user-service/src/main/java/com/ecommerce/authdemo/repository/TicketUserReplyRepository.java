package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.TicketUserReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketUserReplyRepository extends JpaRepository<TicketUserReply, Integer> {
    List<TicketUserReply> findByTicketIdOrderByCreatedAtAsc(Integer ticketId);
}
