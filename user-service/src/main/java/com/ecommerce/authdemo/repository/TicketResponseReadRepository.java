package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.TicketResponseRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketResponseReadRepository extends JpaRepository<TicketResponseRead, Integer> {
    List<TicketResponseRead> findByUserIdOrderByReadAtDesc(Integer userId);

    List<TicketResponseRead> findByResponseIdOrderByReadAtDesc(Integer responseId);

    Optional<TicketResponseRead> findByUserIdAndResponseId(Integer userId, Integer responseId);
}
