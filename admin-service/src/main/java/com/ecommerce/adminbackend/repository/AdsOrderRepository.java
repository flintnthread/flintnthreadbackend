package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AdsOrderRepository extends JpaRepository<AdsOrder, Integer> {

    @Query("""
            SELECT o FROM AdsOrder o
            WHERE (:userId IS NULL OR o.userId = :userId)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(o.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(o.adName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(o.adType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   o.userId IN (
                       SELECT u.id FROM AdsUser u WHERE
                           LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                           LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                           LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                           LOWER(COALESCE(u.company, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   ))
              AND (:status IS NULL OR :status = '' OR LOWER(o.status) = LOWER(:status))
              AND (:billingType IS NULL OR :billingType = '' OR LOWER(o.billingType) = LOWER(:billingType))
            ORDER BY o.id DESC
            """)
    Page<AdsOrder> search(
            @Param("search") String search,
            @Param("status") String status,
            @Param("billingType") String billingType,
            @Param("userId") Integer userId,
            Pageable pageable);

    long countByUserId(Integer userId);

    long countByStatusIgnoreCase(String status);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM AdsOrder o WHERE LOWER(o.status) = 'paid'")
    BigDecimal sumPaidAmount();

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM AdsOrder o WHERE LOWER(o.status) = 'paid' AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal sumPaidAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM AdsOrder o WHERE o.createdAt >= :from AND o.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM AdsOrder o WHERE LOWER(o.status) = LOWER(:status) AND o.createdAt >= :from AND o.createdAt < :to")
    long countByStatusBetween(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    Optional<AdsOrder> findByOrderId(String orderId);
}
