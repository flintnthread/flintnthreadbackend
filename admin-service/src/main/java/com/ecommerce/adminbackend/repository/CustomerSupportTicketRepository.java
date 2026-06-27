package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.CustomerSupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerSupportTicketRepository extends JpaRepository<CustomerSupportTicket, Integer> {

    long countByStatusIgnoreCase(String status);

    @Query("""
            SELECT t FROM CustomerSupportTicket t
            WHERE (:status IS NULL OR LOWER(t.status) = LOWER(:status))
              AND (:type IS NULL OR LOWER(t.type) = LOWER(:type))
              AND (
                    :search IS NULL OR :search = '' OR
                    LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(t.message) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY t.createdAt DESC
            """)
    Page<CustomerSupportTicket> searchTickets(
            @Param("status") String status,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable);
}
