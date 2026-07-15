package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdsPaymentRepository extends JpaRepository<AdsPayment, Integer> {

    @Query("""
            SELECT p FROM AdsPayment p
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(p.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.razorpayPaymentId, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status))
            ORDER BY p.id DESC
            """)
    Page<AdsPayment> search(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM AdsPayment p WHERE LOWER(p.status) IN ('captured', 'paid', 'success')")
    BigDecimal sumSuccessfulAmount();

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM AdsPayment p
            WHERE LOWER(p.status) IN ('captured', 'paid', 'success')
              AND p.createdAt >= :from AND p.createdAt < :to
            """)
    BigDecimal sumSuccessfulAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM AdsPayment p WHERE p.createdAt >= :from AND p.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
