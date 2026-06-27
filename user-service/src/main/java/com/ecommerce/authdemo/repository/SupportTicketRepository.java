package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {

    @Query("""
            SELECT s
            FROM SupportTicket s
            WHERE (:customerId IS NULL OR s.customerId = :customerId)
              AND (:status IS NULL OR LOWER(s.status) = LOWER(:status))
              AND (:type IS NULL OR LOWER(s.type) = LOWER(:type))
            ORDER BY s.createdAt DESC
            """)
    List<SupportTicket> findWithFilters(@Param("customerId") Integer customerId,
                                        @Param("status") String status,
                                        @Param("type") String type);
}
