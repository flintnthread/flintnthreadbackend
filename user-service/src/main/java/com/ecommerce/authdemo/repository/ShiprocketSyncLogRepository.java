package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ShiprocketSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiprocketSyncLogRepository extends JpaRepository<ShiprocketSyncLog, Integer> {
    @Query("""
            SELECT s
            FROM ShiprocketSyncLog s
            WHERE (:orderId IS NULL OR s.orderId = :orderId)
              AND (:orderNumber IS NULL OR LOWER(s.orderNumber) = LOWER(:orderNumber))
              AND (:fromDate IS NULL OR s.createdAt >= :fromDate)
              AND (:toDate IS NULL OR s.createdAt <= :toDate)
            ORDER BY s.createdAt DESC
            """)
    List<ShiprocketSyncLog> findWithFilters(
            @Param("orderId") Integer orderId,
            @Param("orderNumber") String orderNumber,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
