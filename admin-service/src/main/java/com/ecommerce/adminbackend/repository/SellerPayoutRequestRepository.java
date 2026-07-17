package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.SellerPayoutRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SellerPayoutRequestRepository extends JpaRepository<SellerPayoutRequest, Long> {

    Optional<SellerPayoutRequest> findByOrderId(Long orderId);

    List<SellerPayoutRequest> findByOrderIdIn(Collection<Long> orderIds);

    @Query("""
            SELECT p FROM SellerPayoutRequest p
            WHERE (:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status))
            ORDER BY
              CASE WHEN LOWER(p.status) = 'pending' THEN 0 ELSE 1 END,
              p.requestedAt DESC
            """)
    Page<SellerPayoutRequest> findByStatusOptional(@Param("status") String status, Pageable pageable);

    long countByStatusIgnoreCase(String status);

    long countByStatusIgnoreCaseAndRequestedAtAfter(String status, LocalDateTime after);

    List<SellerPayoutRequest> findTop20ByStatusIgnoreCaseOrderByRequestedAtDesc(String status);

    @Query("""
            SELECT COALESCE(SUM(p.requestedAmount), 0) FROM SellerPayoutRequest p
            WHERE LOWER(p.status) = 'paid'
            """)
    java.math.BigDecimal sumPaidAmount();
}
