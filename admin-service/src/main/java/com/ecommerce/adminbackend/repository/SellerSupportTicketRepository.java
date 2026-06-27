package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.SellerSupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerSupportTicketRepository extends JpaRepository<SellerSupportTicket, Long> {

    @Query("""
            SELECT t FROM SellerSupportTicket t
            WHERE (:status IS NULL OR :status = '' OR LOWER(t.status) = LOWER(:status))
              AND (:priority IS NULL OR :priority = '' OR LOWER(t.priority) = LOWER(:priority))
              AND (:search IS NULL OR :search = '' OR
                   LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY t.updatedAt DESC
            """)
    Page<SellerSupportTicket> searchTickets(@Param("status") String status,
                                            @Param("priority") String priority,
                                            @Param("search") String search,
                                            Pageable pageable);

    long countByStatusIgnoreCase(String status);

    long countByPriorityIgnoreCase(String priority);
}
